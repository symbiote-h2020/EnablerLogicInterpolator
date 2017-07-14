package eu.h2020.symbiote.smeur.eli;

import java.util.List;

class QueryTask {
	String taskId;
	int count;
	String location;
	List<String> observedProperties;
	int interval;
	


	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public List<String> getObservedProperties() {
		return observedProperties;
	}

	public void setObservedProperties(List<String> observedProperties) {
		this.observedProperties = observedProperties;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskID(String id) {
		taskId=id;
	}
}

class QueryForResources {
	List<QueryTask> allQueries;
	
	public List<QueryTask> getResources() {
		return allQueries;
	}

	public void setResources(List<QueryTask> list) {
		allQueries=list;
	}
}

//{
//	  "resources": [
//	    {
//	      "taskId": "generated id by enabler logic",
//	      "count": "2",
//	      "location": "symbolicLocation",
//	      "observesProperty": [
//	        "temperature",
//	        "humidity"
//	      ],
//	      "interval": "queryInterval in seconds"
//	    },
//	    {
//	      "taskId": "generated id by enabler logic",
//	      "count": "1",
//	      "location": "symbolicLocation",
//	      "observesProperty": [
//	        "air quality"
//	      ],
//	      "interval": "queryInterval in seconds"
//	    }
//	  ]
//	}