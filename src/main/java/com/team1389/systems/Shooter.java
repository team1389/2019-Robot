package com.team1389.systems;

import com.team1389.command_framework.CommandUtil;
import com.team1389.command_framework.command_base.Command;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.outputs.software.DigitalOut;
import com.team1389.system.Subsystem;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;
import com.team1389.watch.info.BooleanInfo;

public class Shooter extends Subsystem
{
    private DigitalIn beamBreak;
    private DigitalOut leftShooter;
    private DigitalOut rightShooter;

    public Shooter(DigitalOut rightShooter, DigitalOut leftShooter, DigitalIn beamBreak)
    {
        this.rightShooter = rightShooter;
        this.leftShooter = leftShooter;
        this.beamBreak = beamBreak;
    }
    public AddList<Watchable> getSubWatchables(AddList<Watchable> stem)
    {
        return stem.put(scheduler, new BooleanInfo("ball", this::hasBall));
    }
    protected void schedule(Command command)
    {
        super.schedule(command);
    }
    public String getName()
    {
        return "Shooter";
    }
    public void init()
    {

    }
    public void update()
    {

    }
    public void shootRight()
    {
        rightShooter.set(true);
    }
    public Command shootRightCommand()
    {
        return CommandUtil.createCommand(this::shootRight);
    }

    public void shootLeft(){
        leftShooter.set(true);
    }
    public Command shootLeftCommand()
    {
        return CommandUtil.createCommand(this::shootLeft);
    }
    public void resetRightShooter()
    {
        rightShooter.set(false);
    }
    public void resetLeftShooter()
    {
        leftShooter.set(false);
    }
    public Command resetRightShooterCommand()
    {
        return CommandUtil.createCommand(this::resetRightShooter);
    }
    public Command resetLeftShooterCommand()
    {
        return CommandUtil.createCommand(this::resetLeftShooter);
    }
    public boolean hasBall()
    {
        return beamBreak.get();
    }
}