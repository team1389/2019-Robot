package com.team1389.robot;

import java.util.function.Supplier;

import com.team1389.command_framework.CommandUtil;
import com.team1389.command_framework.command_base.Command;
import com.team1389.hardware.inputs.software.DigitalIn;
import com.team1389.hardware.outputs.software.DigitalOut;
import com.team1389.system.Subsystem;
import com.team1389.util.list.AddList;
import com.team1389.watch.Watchable;
import com.team1389.watch.info.BooleanInfo;
import com.team1389.watch.info.EnumInfo;

import sun.tools.tree.CommaExpression;

public class Shooter extends Subsystem
{
    private State shooterState;
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
        return stem.put(new EnumInfo("shooter state", () -> shooterState), scheduler, new BooleanInfo("ball", this::hasBall));
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
        if(hasBall())
        {
            enterState(State.CARRYING);
        }
        else
        {
            enterState(State.EMPTY);
        }
    }
    public void update()
    {

    }
    public State getState()
    {
        return this.shooterState;
    }
    public enum State
    {
        CARRYING, EMPTY
    }
    public void enterState(State state)
    {
        if(state == shooterState){
            return;
        }
        switch(state)
        {
        case EMPTY:
            return (resetShooter());
        case CARRYING:
            return (shootLeftCommand());
        }
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
    public void resetShooter()
    {
        rightShooter.set(false);
        leftShooter.set(false);
    }
    
    public Command resetShooterCommand()
    {
        return CommandUtil.createCommand(this::resetShooter);
    }
    public boolean hasBall()
    {
        return beamBreak.get();
    }
}