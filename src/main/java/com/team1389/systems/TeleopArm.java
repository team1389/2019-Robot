package com.team1389.systems;

import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.inputs.software.RangeIn;
import com.team1389.hardware.outputs.software.DigitalOut;
import com.team1389.hardware.outputs.software.RangeOut;
import com.team1389.hardware.value_types.Percent;
import com.team1389.hardware.value_types.Position;
import com.team1389.system.Subsystem;
import com.team1389.systems.Arm.State;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;

public class TeleopArm extends Subsystem
{
    // output
    private DigitalOut hatchOuttake;
    private DigitalOut cargoLauncher;
    private RangeOut<Percent> cargoIntake;
    private RangeOut<Percent> arm;

    // sensors
    private DigitalIn cargoIntakeBeamBreak;
    private RangeIn<Position> armAngle;

    // control
    private RangeIn<Percent> armAxis;
    private DigitalIn intakeHatchBtn;
    private DigitalIn intakeCargoBtn;
    private DigitalIn storeCargoBtn;
    private DigitalIn outtakeCargoBtn;
    private DigitalIn outtakeHatchBtn;

    private boolean useBeamBreak = true;

    private Arm armSystem;

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
     * @param armAxis
     *                                 input for controlling arm
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
    public TeleopArm(DigitalOut hatchOuttake, DigitalOut cargoLauncher, RangeOut<Percent> cargoIntake,
            RangeOut<Percent> arm, DigitalIn cargoIntakeBeamBreak, RangeIn<Position> armAngle, RangeIn<Percent> armAxis,
            DigitalIn outtakeHatchBtn, DigitalIn outtakeCargoBtn, DigitalIn intakeHatchBtn, DigitalIn intakeCargoBtn,
            DigitalIn storeCargoBtn, boolean useBeamBreak)
    {
        this.hatchOuttake = hatchOuttake;
        this.cargoLauncher = cargoLauncher;
        this.cargoIntake = cargoIntake;
        this.arm = arm;
        this.cargoIntakeBeamBreak = cargoIntakeBeamBreak;
        this.armAxis = armAxis;
        this.outtakeHatchBtn = outtakeHatchBtn;
        this.intakeCargoBtn = intakeCargoBtn;
        this.outtakeCargoBtn = outtakeCargoBtn;
        this.storeCargoBtn = storeCargoBtn;
        this.useBeamBreak = useBeamBreak;
    }

    @Override
    public void init()
    {
        armSystem = new Arm(hatchOuttake, cargoLauncher, cargoIntake, arm, cargoIntakeBeamBreak, armAngle);
    }

    @Override
    public void update()
    {
        advancedUpdate();
        // I don't know how to add manual control as backup without having
        // control be really confusing
    }

    private void advancedUpdate()
    {
        if (intakeHatchBtn.get())
        {
            armSystem.enterState(State.HATCH_PICK_UP);
        }
        else if (intakeCargoBtn.get())
        {
            armSystem.enterState(State.CARGO_PICK_UP);
        }
        else if (outtakeCargoBtn.get())
        {
            armSystem.enterState(State.OUTTAKE_CARGO);
        }
        else if (outtakeHatchBtn.get())
        {
            armSystem.enterState(State.OUTTAKE_HATCH);
        }
        else if (storeCargoBtn.get())
        {
            armSystem.enterState(State.STORE_CARGO);
        }
        armSystem.update();
    }

    @Override
    public String getName()
    {
        return "Teleop Arm";
    }

    // TODO: add watchables, figure out how to do this right
    @Override
    public AddList<Watchable> getSubWatchables(AddList<Watchable> arg0)
    {
        return arg0;
    }
}