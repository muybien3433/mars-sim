/*
 * Mars Simulation Project
 * TaskManager.java
 * @date 2022-06-24
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task.util;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mars_sim.msp.core.SimulationFiles;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.data.SolListDataLogger;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.task.Walk;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MarsClockFormat;
import org.mars_sim.msp.core.time.Temporal;
import org.mars_sim.msp.core.vehicle.Vehicle;

/*
 * The TaskManager class keeps track of a Worker's current task and can randomly
 * assign a new task based on a list of possible tasks and the current situation.
 */
public abstract class TaskManager implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(TaskManager.class.getName());
	
	/*
	 * This class represents a record of a given activity (task or mission)
	 * undertaken by a person or a robot
	 */
	public final class OneActivity implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		private String taskName;
		private String missionName;
		private String description;
		private String phase;
		private double startTime;


		public OneActivity(double startTime, String taskName, String description, String phase, String missionName) {
			this.taskName = taskName;
			this.missionName = missionName;
			this.description = description;
			this.startTime = startTime;
			this.phase = phase;
		}

		/**
		 * Gets the start time of the task.
		 * 
		 * @return start time
		 */
		public double getStartTime() {
			return startTime;
		}

		/**
		 * Gets the task name.
		 * 
		 * @return task name
		 */
		public String getTaskName() {
			return taskName;
		}

		/**
		 * Gets the description what the actor is doing.
		 * 
		 * @return description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Gets the task phase.
		 * 
		 * @return task phase
		 */
		public String getPhase() {
			return phase;
		}

		public String getMission() {
			return missionName;
		}
	}

	/** Number of days to record Tack Activities. */
	public static final int NUM_SOLS = 7;
	
	/** Amount of millisols elapsed before the task cache is rebuilt. */
	private static final double TASK_MILLISOLS  = 0.1;
	
	protected static MarsClock marsClock;

	private static PrintWriter diagnosticFile = null;

	/**
	 * Enable the detailed diagnostics
	 * @throws FileNotFoundException 
	 */
	public static void setDiagnostics(boolean diagnostics) throws FileNotFoundException {
		if (diagnostics) {
			if (diagnosticFile == null) {
				String filename = SimulationFiles.getLogDir() + "/task-cache.txt";
				diagnosticFile  = new PrintWriter(filename);
				logger.config("Diagnostics enabled to " + filename);
			}
		}
		else if (diagnosticFile != null){
			diagnosticFile.close();
			diagnosticFile = null;
		}
	}

	/**The worker **/
	protected transient Unit worker;
	/** The current task the worker is doing. */
	protected Task currentTask;
	/** The last task the person was doing. */
	private Task lastTask;

	private transient TaskCache taskProbCache = null;

	// Data members
	/** The timestamp (with 2 decimal place) of the task to be recorded. */
	private double now = -1;
	/** The cache for msol. */
	private double msolCache = -1.0;



	/** The history of tasks. */
	private SolListDataLogger<OneActivity> allActivities;
	/** The last activity. */
	private OneActivity lastActivity = null;
	/** The list of pending of tasks. */
	private List<String> pendingTasks;
	

	protected TaskManager(Unit worker) {
		this.worker = worker;
		allActivities = new SolListDataLogger<>(NUM_SOLS);
		pendingTasks = new CopyOnWriteArrayList<>();
	}

	/**
	 * Returns true if person has a task (may be inactive).
	 * 
	 * @return true if person has a task
	 */
	public boolean hasTask() {
		return currentTask != null;
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskName() {
		if (currentTask != null) {
			return currentTask.getName();
		} else {
			return "";
		}
	}

	public String getSubTaskName() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			return currentTask.getSubTask().getName();
		} else {
			return "";
		}
	}

	public String getSubTask2Name() {
		Task task = getRealTask();
		if (task != null) {
			return task.getName();
		} else {
			return "";
		}
	}

	/**
	 * Gets the bottom-most real-time task. 
	 * 
	 * @return
	 */
	public Task getRealTask() {
		if (currentTask == null) {
			return null;
		}
		
		Task subtask1 = currentTask.getSubTask();
		if (subtask1 == null) {
			return currentTask;
		}
		
		if (subtask1.getSubTask() == null) {
			return subtask1;
		}
		
		Task subtask2 = subtask1.getSubTask();
		if (subtask2 == null) {
			return subtask1;
		}
		
		if (subtask2.getSubTask() == null) {
			return subtask2;
		}
		
		return subtask2.getSubTask();
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskClassName() {
		if (currentTask != null) {
			return currentTask.getTaskSimpleName();
		} else {
			return "";
		}
	}

	/**
	 * Returns a description of current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return a description of the current task
	 */
	public String getTaskDescription(boolean subTask) {
		if (currentTask != null) {
			return currentTask.getDescription(subTask);
		}
		return "";
	}

	public String getSubTaskDescription() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			String t = currentTask.getSubTask().getDescription();
			if (t != null && !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	public String getSubTask2Description() {
		if (currentTask != null && currentTask.getSubTask() != null
				&& currentTask.getSubTask().getSubTask() != null) {
			String t = currentTask.getSubTask().getSubTask().getDescription();
			if (t != null) // || !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	/**
	 * Returns the current task phase if there is one. Returns null if current task
	 * has no phase. Returns null if there is no current task.
	 * 
	 * @return the current task phase
	 */
	public TaskPhase getPhase() {
		if (currentTask != null) {
			return currentTask.getPhase();
		} else {
			return null;
		}
	}

	public TaskPhase getSubTaskPhase() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			return currentTask.getSubTask().getPhase();
		} else {
			return null;
		}
	}

	public TaskPhase getSubTask2Phase() {
		if (currentTask != null && currentTask.getSubTask() != null
				&& currentTask.getSubTask().getSubTask() != null) {
			return currentTask.getSubTask().getSubTask().getPhase();
		} else {
			return null;
		}
	}

	/**
	 * Returns the current task. Return null if there is no current task.
	 * 
	 * @return the current task
	 */
	public Task getTask() {
		return currentTask;
	}

	public String getLastTaskName() {
		return (lastTask != null ? lastTask.getTaskSimpleName() : "");
	}

	public String getLastTaskDescription() {
		return (lastTask != null ? lastTask.getDescription() : "");
	}

	/**
	 * Returns true if person has an active task.
	 * 
	 * @return true if person has an active task
	 */
	public boolean hasActiveTask() {
		return (currentTask != null && !currentTask.isDone());
	}

	/**
	 * Ends all sub tasks.
	 */
	public void endSubTask() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			currentTask.getSubTask().endTask();
		}
	}

	/**
	 * Starts a new task.
	 * 
	 * @param newTask
	 */
	public void startTask(Task newTask) {
		if (newTask != null) {
			// Save the current task as last task
			lastTask = currentTask;
			
			// End the current task properly
			if ((currentTask != null) && !currentTask.isDone()) {
				String des = currentTask.getDescription();
	
				logger.info(worker, 20_000, "Quit '" + des + "' to start new Task '"
							+ newTask.getDescription() + "'.");
				currentTask.endTask();
			}
			
			// Make the new task as the current task
			currentTask = newTask;
			
			// Send out the task event
			worker.fireUnitUpdate(UnitEventType.TASK_EVENT, newTask);
		}
	}
	
	/**
	 * Adds a task to the stack of tasks.
	 * 
	 * @param newTask the task to be added
	 */
	public boolean addTask(Task newTask) {
		
		if (newTask == null) {
			return false;
		}
		
		if (hasActiveTask()) {
			String currentDes = currentTask.getDescription();

			// Note: make sure robot's 'Sleep Mode' won't return false
			if (currentDes.contains("Sleeping"))
				return false;
			
			if (currentDes.contains("EVA"))
				return false;
			
			if (newTask.getDescription().equalsIgnoreCase(currentDes))
				return false;	
		}
		
		startTask(newTask);
		
		return true;
	}

	/**
	 * Sets the current task to null.
	 * @param reason May ebe used in an override method
	 */
	public void clearAllTasks(String reason) {
		endCurrentTask();
	}

	/**
	 * Ends the current task.
	 */
	public void endCurrentTask() {
		if (currentTask != null) {
			currentTask.endTask();
			currentTask = null;
			worker.fireUnitUpdate(UnitEventType.TASK_EVENT);
		}
	}

	/**
	 * Clears a specific task.
	 * 
	 * @param taskString
	 */
	public void clearSpecificTask(String taskString) {
		
		Task subTask1 = currentTask.getSubTask();
		
		if (currentTask != null && subTask1 != null) {
			
			Task subTask2 = subTask1.getSubTask();
			
			if (subTask2 != null) {
				String taskName2 = subTask2.getClass().getSimpleName();
				if (taskName2.equalsIgnoreCase(taskString)) {
					subTask2.endTask();
				}
			}
			
			else {				
				String taskName1 = subTask1.getClass().getSimpleName();
				if (taskName1.equalsIgnoreCase(taskString)) {
					subTask1.endTask();
				}
			}
		}
		
		else {
			String taskName0 = currentTask.getClass().getSimpleName();
			if (taskName0.equalsIgnoreCase(taskString)) {
				endCurrentTask();
			}
		}
	}

	/**
	 * Re-initializes instances when loading from a saved sim
	 */
	public void reinit() {
		if (currentTask != null)		
			currentTask.reinit();
		if (lastTask != null)
			lastTask.reinit();
	}

	/**
	 * Checks if task probability cache should be used.
	 * 
	 * @return true if cache should be used.
	 */
	protected boolean useCache() {
		double msol = marsClock.getMillisol();
		double diff = msol - msolCache;
		if (diff > TASK_MILLISOLS) {
			msolCache = msol;
			return false;
		}
		return true;
	}

	/**
	 * Calculates and caches the probabilities.
	 * 
	 * This will NOT use the cache but assumes the callers know when a cahce can be used or not used. 
	 */
	protected abstract TaskCache rebuildTaskCache();

	/**
	 * Start a new task for the worker based on tasks available at their location.
	 * Uses the task probability cache. If a task is found; then it is assigned
	 * to the manager to start working.
	 */
	public void startNewTask() {
		Task selectedTask = null;
		MetaTask selectedMetaTask = null;

		// If cache is not current, calculate the probabilities.
		if (!useCache()) {
			taskProbCache = rebuildTaskCache();
			
			// Output shift
			if (diagnosticFile != null) {
				outputCache(taskProbCache);
			}
		}

		if (taskProbCache.getTasks().isEmpty()) {
			// SHhould never happen since TaskManagers have to return a populated list
			// with doable defaults if needed
			logger.severe(worker, "No normal Tasks available.");
			return;
		}
		else {
			selectedMetaTask = taskProbCache.getRandomSelection();
		}

		// Call constructInstance of the selected Meta Task to commence the ai task
		selectedTask = createTask(selectedMetaTask);

		// Start this new task
		startTask(selectedTask);

		// Clear time cache.
		msolCache = -1;	
	}

	/**
	 * Constructs a new Task of the specified type.
	 * 
	 * @param selectedMetaTask Type of task to create.
	 * @return New Task.
	 */
	protected abstract Task createTask(MetaTask selectedMetaTask);

	/**
	 * Returns the last calculated probability map.
	 * 
	 * @return
	 */
	public TaskCache getLatestTaskProbability() {
		return taskProbCache;
	}
	
	/**
	 * Outputs the cache to a file for diagnostics.
	 * 
	 * @param extras Extra details about Task
	 */
	private void outputCache(TaskCache current) {	
		synchronized (diagnosticFile) {	
			diagnosticFile.println(MarsClockFormat.getDateTimeStamp(marsClock));
			diagnosticFile.println("Worker:" + worker.getName());
			diagnosticFile.println(current.getContext());				
			diagnosticFile.println("Total:" + current.getTotal());
			for (Entry<MetaTask, Double> task : taskProbCache.getTasks().entrySet()) {
				diagnosticFile.println(task.getKey().getName() + ":" + task.getValue());
			}
			
			diagnosticFile.println();
			diagnosticFile.flush();
		}
	}

	/**
	 * Time has advanced on. This has to carry over the last Activity of yesterday into today.
	 */
	public boolean timePassing(ClockPulse pulse) {
		// Create a timestamp with 2 decimal place
		now = Math.round(pulse.getMarsTime().getMillisol() * 100.0)/100.0;
		
		// New day so the Activity at the end of yesterday has to be carried over to the 1st of today
		if (pulse.isNewSol() && lastActivity != null) {
			// Save the first activity at the start of the day
			// Note: it could be the previous activity from previous day
			OneActivity firstActivity = new OneActivity(0,
											lastActivity.getTaskName(),
											lastActivity.getDescription(),
											lastActivity.getPhase(),
											lastActivity.getMission());
			allActivities.addData(firstActivity);
		}
		return true;
	}
	
	/**
	 * Records a task onto the schedule.
	 * 
	 * @param changed The active task.
	 * @param mission Associated mission.
	 */
	void recordTask(Task changed, String mission) {
		double newStartTime = now;
		String newDescription = changed.getDescription();
		String newPhase = "";
		if (changed.getPhase() != null)
			newPhase = changed.getPhase().getName();
		
		if (!newDescription.equals("") && (lastActivity == null 
				|| !newDescription.equals(lastActivity.description)
				|| !newPhase.equals(lastActivity.phase))) {
			
			OneActivity newActivity = new OneActivity(newStartTime, 
												changed.getName(false),
												newDescription,
												newPhase, 
												mission);

			allActivities.addData(newActivity);
			lastActivity = newActivity;
		}
	}
	
	/**
	 * Gets the today's activities.
	 * 
	 * @return a list of today's activities
	 */
	public List<OneActivity> getTodayActivities() {
		return allActivities.getTodayData();
	}
	
	/**
	 * Gets all activities of all days a person.
	 * 
	 * @return all activity schedules
	 */
	public Map<Integer, List<OneActivity>> getAllActivities() {
		return allActivities.getHistory();
	}
	
	/**
	 * Checks if the person or robot is walking through a given vehicle.
	 * 
	 * @param vehicle the vehicle.
	 * @return true if walking through vehicle.
	 */
	public boolean isWalkingThroughVehicle(Vehicle vehicle) {
	
	    boolean result = false;
	
	    Task task = currentTask;
	    while ((task != null) && !result) {
	        if (task instanceof Walk) {
	            Walk walkTask = (Walk) task;
	            if (walkTask.isWalkingThroughVehicle(vehicle)) {
	                result = true;
	            }
	        }
	        task = task.getSubTask();
	    }
	
	    return result;
	}

	/**
	 * Checks if the person or robot is walking through a given building.
	 * 
	 * @param building the building.
	 * @return true if walking through building.
	 */
	public boolean isWalkingThroughBuilding(Building building) {
	
		boolean result = false;
	
		Task task = currentTask;
		while ((task != null) && !result) {
			if (task instanceof Walk) {
				Walk walkTask = (Walk) task;
				if (walkTask.isWalkingThroughBuilding(building)) {
					result = true;
				}
			}
			task = task.getSubTask();
		}
	
		return result;
	}
	
	/**
	 * Gets all pending tasks
	 *
	 * @return
	 */
	public List<String> getPendingTasks() {
		return pendingTasks;
	}
	
	/**
	 * Adds a pending task if it is not in the pendingTask list yet.
	 *
	 * @param task
	 * @param allowDuplicate
	 * @return
	 */
	public boolean addAPendingTask(String task, boolean allowDuplicate) {
		if (allowDuplicate || !pendingTasks.contains(task)) {
			pendingTasks.add(task);
			logger.info(worker, 20_000L, "Given a new task order of '" + task + "'.");
			return true;
		}

		return false;
	}

	/**
	 * Deletes a pending task
	 *
	 * @param task
	 */
	public void deleteAPendingTask(String task) {
		pendingTasks.remove(task);
		logger.info(worker, "Removed the task order of '" + task + "'.");
	}

	/**
	 * Gets the first pending meta task in the queue
	 *
	 * @return
	 */
	protected MetaTask getAPendingMetaTask() {
		if (!pendingTasks.isEmpty()) {
			String firstTask = pendingTasks.get(0);
			pendingTasks.remove(firstTask);
			return convertTask2MetaTask(firstTask);
		}
		return null;
	}

	/**
	 * Converts a task to its corresponding meta task
	 *
	 * @param a task
	 */
	private static MetaTask convertTask2MetaTask(String task) {
		return MetaTaskUtil.getMetaTask(task.replaceAll(" ","") + "Meta");
	}
	
	/**
	 * Checks if the worker is currently performing this task.
	 * 
	 * @param task
	 * @return
	 */
	public boolean hasSameTask(String task) {
		if (getTaskName().equalsIgnoreCase(task))
			return true;
		
		return false;
	}
	
	/**
	 * Reloads instances after loading from a saved sim.
	 * 
	 * @param clock
	 */
	public static void initializeInstances(MarsClock clock) {
		marsClock = clock;
	}

}
