package com.team1389.systems;

import com.team1389.command_framework.CommandUtil;
import com.team1389.command_framework.command_base.Command;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.outputs.software.DigitalOut;
import com.team1389.hardware.outputs.software.PercentOut;
import com.team1389.system.Subsystem;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;
import com.team1389.watch.info.BooleanInfo;

public class Climber extends Subsystem
{
    private DigitalIn bumpSwitch;
    private PercentOut wheelVoltage;
    private DigitalOut liftPiston;

    public Climber(DigitalOut liftPiston, PercentOut wheelVoltage, DigitalIn bumpSwitch)
    {
        this.wheelVoltage = wheelVoltage;
        this.liftPiston = liftPiston;
        this.bumpSwitch = bumpSwitch;
    }
    public AddList<Watchable> getSubWatchables(AddList<Watchable> stem)
    {
        return stem.put(scheduler, new BooleanInfo("switch", this::switchBumped));
    }
    protected void schedule(Command command)
    {
        super.schedule(command);
    }
    public String getName()
    {
        return "Climber";
    }
    public void init()
    {

    }
    public void update()
    {

    }
    public void climb()
    {
        liftPiston.set(true);
        wheelVoltage.set(.05);
    }
    public Command climbCommand()
    {
        return CommandUtil.createCommand(this::climb);
    }
    public void retract()
    {
        liftPiston.set(false);
        wheelVoltage.set(0);
    }
    public Command retractCommand()
    {
        return CommandUtil.createCommand(this::retract);        
    }
    public boolean switchBumped()
    {
        return bumpSwitch.get();
    }
    public void autoRetract()
    {
        if(switchBumped() == true)
        {
            retract();
        }
    }
}