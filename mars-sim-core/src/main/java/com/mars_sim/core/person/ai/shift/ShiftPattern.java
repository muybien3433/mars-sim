/*
 * Mars Simulation Project
 * ShiftPattern.java
 * @date 2022-11-18
 * @author Barry Evans
 */
package com.mars_sim.core.person.ai.shift;

import java.util.List;

/**
 * This class represents the definition of a Shift Pattern template. 
 * It consists of a number of Shifts.
 */
public class ShiftPattern {

    private List<ShiftSpec> shifts;
    private String name;
    private int rotationSols;
    private int leavePerc;

    public ShiftPattern(String name, List<ShiftSpec> shifts, int rotationSols, int leavePerc) {
        this.name = name;
        this.shifts = shifts;
        this.rotationSols = rotationSols;
        this.leavePerc = leavePerc;
    }

    public List<ShiftSpec> getShifts() {
        return shifts;
    }

    public String getName() {
        return name;
    }

    public int getLeavePercentage() {
        return leavePerc;
    }

    public int getRotationSols() {
        return rotationSols;
    }

}
