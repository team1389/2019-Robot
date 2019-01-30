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

public class Arm extends Subsystem
{
    // Closed-loop control
    private SynchronousPIDController<Percent, Position> controller;
    private PIDConstants pidConstants;
    private final int TOLERANCE_IN_DEGREES = 3;

    State currentState;

    // output
    private DigitalOut hatchOuttake;
    private DigitalOut cargoLauncher;
    private RangeOut<Percent> cargoIntake;
    private RangeOut<Percent> arm;

    // sensors
    private DigitalIn cargoIntakeBeamBreak;

    // control
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
     *                                 cargo is in the intake
     * @param outtakeHatchBtn
     *                                 input for triggering outtaking hatch
     * @param intakeCargoBtn
     *                                 input for triggering cargo intake
     * @param outtakeCargoBtn
     *                                 input for triggering cargo outtake
     * @param useBeamBreak
     *                                 toggle for whether or not to use the beam
     *                                 break
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
        controller.update();
    }

    @Override
    public String getName()
    {
        return "Arm";
    }

    // TODO: add watchables, try to keep from overlapping
    @Override
    public AddList<Watchable> getSubWatchables(AddList<Watchable> arg0)
    {
        return arg0;
    }

    public enum State
    {
        HATCH_PICK_UP(-15), CARGO_PICK_UP(-15), STORE_CARGO(115), PASSIVE(115), OUTTAKE_CARGO(45), OUTTAKE_HATCH(90);
        // assumes 0 is when arm is parallel with the robot top
        private double angle;

        private State(double angle)
        {
            this.angle = angle;
        }
    }

    public void enterState(State desiredState)
    {
        reset();
        switch (desiredState)
        {
        case HATCH_PICK_UP:
            controller.setSetpoint(State.HATCH_PICK_UP.angle);
            Command hatchPickUp = CommandUtil.combineSequential(extendHatchPistonsCommand(false),
                    controller.getPIDToCommand(TOLERANCE_IN_DEGREES));
            scheduler.schedule(hatchPickUp);
            break;
        case CARGO_PICK_UP:
            controller.setSetpoint(State.CARGO_PICK_UP.angle);
            Command cargoPickUp = CommandUtil.combineSequential(extendHatchPistonsCommand(false),
                    extendCargoPistonsCommand(false), controller.getPIDToCommand(TOLERANCE_IN_DEGREES),
                    intakeCargoCommand());
            scheduler.schedule(cargoPickUp);
            break;
        case STORE_CARGO:
            controller.setSetpoint(State.STORE_CARGO.angle);
            Command storeCargo = CommandUtil.combineSequential(controller.getPIDToCommand(TOLERANCE_IN_DEGREES),
                    outtakeCargoCommand());
            scheduler.schedule(storeCargo);
            break;
        case OUTTAKE_CARGO:
            controller.setSetpoint(State.OUTTAKE_CARGO.angle);
            Command outtakeCargo = CommandUtil.combineSequential(controller.getPIDToCommand(TOLERANCE_IN_DEGREES),
                    extendCargoPistonsCommand(true), outtakeCargoCommand());
            scheduler.schedule(outtakeCargo);
            break;
        case OUTTAKE_HATCH:
            controller.setSetpoint(State.OUTTAKE_HATCH.angle);
            Command outtakeHatch = CommandUtil.combineSequential(controller.getPIDToCommand(TOLERANCE_IN_DEGREES),
                    extendHatchPistonsCommand(true));
            scheduler.schedule(outtakeHatch);
            break;
        }
        // should always go back to passive after finishing other commands
        scheduler.schedule(goToPassiveCommand());
    }

    private void reset()
    {
        scheduler.cancelAll();
        arm.set(0);
        cargoIntake.set(0);
    }

    private Command goToPassiveCommand()
    {
        return CommandUtil.combineSequential(
                CommandUtil.createCommand(() -> controller.setSetpoint(State.PASSIVE.angle)),
                controller.getPIDToCommand(TOLERANCE_IN_DEGREES));
    }

    private Command extendHatchPistonsCommand(boolean extend)
    {
        return CommandUtil.createCommand(() -> hatchOuttake.set(extend));
    }

    private Command extendCargoPistonsCommand(boolean extend)
    {
        return CommandUtil.createCommand(() -> cargoLauncher.set(extend));
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
        };
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
        };
    }

}