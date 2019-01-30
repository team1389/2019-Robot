package com.team1389.systems;

import com.team1389.command_framework.CommandUtil;
import com.team1389.command_framework.command_base.Command;
import com.team1389.configuration.PIDConstants;
import com.team1389.controllers.SynchronousPIDController;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.inputs.software.RangeIn;
import com.team1389.hardware.outputs.software.DigitalOut;
import com.team1389.hardware.outputs.software.RangeOut;
import com.team1389.hardware.value_types.Percent;
import com.team1389.hardware.value_types.Position;
import com.team1389.system.Subsystem;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;
import com.team1389.watch.info.StringInfo;

/**
 * implements autonomous control of arm subsystem
 */
public class Arm extends Subsystem
{
    // Closed-loop control
    private SynchronousPIDController<Percent, Position> controller;
    private PIDConstants pidConstants;
    private final int TOLERANCE_IN_DEGREES = 3;

    private State currentState;

    // output
    private DigitalOut hatchOuttake;
    private DigitalOut cargoLauncher;
    private RangeOut<Percent> cargoIntake;
    private RangeOut<Percent> arm;

    // sensors
    private DigitalIn cargoIntakeBeamBreak;
    private RangeIn<Position> armAngle;

    /**
     * 
     * @param hatchOuttake
     *                                 controller for hatch detach mechanism
     * @param cargoLauncher
     *                                 controller for piston that hits ball into
     *                                 intake
     * @param cargoIntake
     *                                 controller for flywheel intake
     * @param arm
     *                                 controller for arm motion
     * @param cargoIntakeBeamBreak
     *                                 input from beam break that detects if
     *                                 cargo is in the intake (Must be true when
     *                                 it detects)
     * @param armAngle
     *                                 gives angle of the arm in degrees
     */
    public Arm(DigitalOut hatchOuttake, DigitalOut cargoLauncher, RangeOut<Percent> cargoIntake, RangeOut<Percent> arm,
            DigitalIn cargoIntakeBeamBreak, RangeIn<Position> armAngle)
    {
        this.hatchOuttake = hatchOuttake;
        this.cargoLauncher = cargoLauncher;
        this.cargoIntake = cargoIntake;
        this.arm = arm;
        this.cargoIntakeBeamBreak = cargoIntakeBeamBreak;
        this.armAngle = armAngle;

    }

    @Override
    public void init()
    {
        pidConstants = new PIDConstants(0.01, 0, 0);
        controller = new SynchronousPIDController<Percent, Position>(pidConstants, armAngle, arm);
        controller.setInputRange(-15, 115);
        currentState = State.PASSIVE;
        enterState(currentState);
    }

    @Override
    public void update()
    {
        // I don't think I have to update the pid controller seperately
        scheduler.update();
    }

    @Override
    public String getName()
    {
        return "Arm";
    }

    @Override
    public AddList<Watchable> getSubWatchables(AddList<Watchable> arg0)
    {
        return arg0.put(new StringInfo("arm state", () -> currentState.name), scheduler);
    }

    public enum State
    {
        INTAKE_HATCH(-15, "Intake Hatch"), INTAKE_CARGO(-15, "Intake Cargo"), PREPING_FOR_SHOOT(115,
                "Preparing for shoot "), PASSIVE(115,
                        "Passive"), OUTTAKE_CARGO(45, "Outtake Cargo"), OUTTAKE_HATCH(90, "Outtake Hatch");
        // assumes 0 is when arm is parallel with the robot top
        private double angle;
        private String name;

        private State(double angle, String name)
        {
            this.angle = angle;
            this.name = name;
        }
    }

    // Probably need wait times before outtaking for most of these
    public void enterState(State desiredState)
    {
        reset();
        switch (desiredState)
        {
        case INTAKE_HATCH:
            controller.setSetpoint(State.INTAKE_HATCH.angle);
            Command hatchPickUp = CommandUtil.combineSequential(extendHatchPistonsCommand(false),
                    controller.getPIDToCommand(TOLERANCE_IN_DEGREES)).setName("move and intake hatch");
            scheduler.schedule(hatchPickUp);
            break;
        case INTAKE_CARGO:
            controller.setSetpoint(State.INTAKE_CARGO.angle);
            Command cargoPickUp = CommandUtil
                    .combineSequential(extendHatchPistonsCommand(false), extendCargoPistonsCommand(false),
                            controller.getPIDToCommand(TOLERANCE_IN_DEGREES), intakeCargoCommand())
                    .setName("move and intake cargo");
            scheduler.schedule(cargoPickUp);
            break;
        case PREPING_FOR_SHOOT:
            controller.setSetpoint(State.PREPING_FOR_SHOOT.angle);
            Command storeCargo = CommandUtil
                    .combineSequential(controller.getPIDToCommand(TOLERANCE_IN_DEGREES), outtakeCargoCommand())
                    .setName("Prepare for shooting");
            scheduler.schedule(storeCargo);
            break;
        case OUTTAKE_CARGO:
            controller.setSetpoint(State.OUTTAKE_CARGO.angle);
            Command outtakeCargo = CommandUtil.combineSequential(controller.getPIDToCommand(TOLERANCE_IN_DEGREES),
                    extendCargoPistonsCommand(true), outtakeCargoCommand()).setName("move and outtake cargo");
            scheduler.schedule(outtakeCargo);
            break;
        case OUTTAKE_HATCH:
            controller.setSetpoint(State.OUTTAKE_HATCH.angle);
            Command outtakeHatch = CommandUtil.combineSequential(controller.getPIDToCommand(TOLERANCE_IN_DEGREES),
                    extendHatchPistonsCommand(true)).setName("move and outtake hatch");
            scheduler.schedule(outtakeHatch);
            break;
        case PASSIVE:
            // you don't do anything because you should never be manually
            // entering passive
            break;
        }
        // should always go back to passive after finishing other commands
        scheduler.schedule(goToPassiveCommand());
    }

    public void reset()
    {
        scheduler.cancelAll();
        arm.set(0);
        cargoIntake.set(0);
    }

    private Command goToPassiveCommand()
    {
        return CommandUtil
                .combineSequential(CommandUtil.createCommand(() -> controller.setSetpoint(State.PASSIVE.angle)),
                        controller.getPIDToCommand(TOLERANCE_IN_DEGREES))
                .setName("Go To Passive");
    }

    private Command extendHatchPistonsCommand(boolean extend)
    {
        return CommandUtil.createCommand(() -> hatchOuttake.set(extend)).setName("extend hatch piston");
    }

    private Command extendCargoPistonsCommand(boolean extend)
    {
        return CommandUtil.createCommand(() -> cargoLauncher.set(extend)).setName("extend cargo piston");
    }

    private Command intakeCargoCommand()
    {
        return new Command()
        {
            @Override
            protected boolean execute()
            {
                cargoIntake.set(-1);
                return cargoIntakeBeamBreak.get();
            }

            @Override
            protected void done()
            {
                cargoIntake.set(0);
            }

        }.setName("intake cargo");
    }

    private Command outtakeCargoCommand()
    {
        return new Command()
        {
            @Override
            protected boolean execute()
            {
                cargoIntake.set(1);
                return !cargoIntakeBeamBreak.get();
            }

            @Override
            protected void done()
            {
                cargoIntake.set(0);
            }
        }.setName("outtake cargo");
    }

}