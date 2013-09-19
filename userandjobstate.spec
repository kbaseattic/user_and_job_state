/* 
Service for storing arbitrary key/object pairs on a per user per service basis
and storing job status so that a) long JSON RPC calls can report status and
UI elements can receive updates, and b) there's a centralized location for 
job status reporting.

The service assumes other services are capable of simple math and does not
throw errors if a progress bar overflows.

Currently devs are on the honor system not to clobber each other's settings and
jobs. If necessary, per Steve Chan we could set up service authentication by
passing a token in the arguments.

Setting objects are limited to 1Mb.

Potential process flows:

Asysnc:
UI calls service function which returns with job id
service call [spawns thread/subprocess to run job that] periodically updates
	the job status of the job id on the job status server
meanwhile, the UI periodically polls the job status server to get progress
	updates
service call finishes, completes job
UI pulls pointers to results from the job status server

Sync:
UI creates job, gets job id
UI starts thread that calls service, providing job id
service call runs, periodically updating the job status of the job id on the
	job status server
meanwhile, the UI periodically polls the job status server to get progress
	updates
service call finishes, completes job, returns results
UI thread joins

mongodb structures:

State collection:
{
	_id:
	user:
	service:
	key: (unique index on user/service/key)
	value:
}

Job collection:
{
	_id:
	user:
	service:
	desc:
	progtype: ('percent', 'task', 'none')
	prog: (int)
	maxprog: (int, 100 for percent, user specified for task)
	status: (user supplied string)
	updated: (date)
	complete: (bool) (index on user/service/complete)
	error: (bool)
	result: {
		shocknodes: (list of strings)
		shockurl:
		workspaceids: (list of strings)
		workspaceurl:
	}
}
*/

module UserAndJobState {

	/* All calls require authentication. */
	authentication required;
	
	/* A service name. */
	typedef string service_name;
	
	/* Set the state of a key for a service. */
	funcdef set_state(service_name service, string key,
		UnspecifiedObject value) returns();
		
	/* Get the state of a key for a service. */
	funcdef get_state(service_name service, string key)
		returns(UnspecifiedObject value);
	
	/* Remove a key value pair. */
	funcdef remove_state(service_name service, string key) returns ();
		
	/* List all keys. */
	funcdef list_state(service_name service) returns(
		list<string> keys);

	/* A boolean. 0 = false, other = true. */
	typedef int boolean;
	
	/* A time, e.g. 2012-12-17T23:24:06. */
	typedef string timestamp;
		
	/* A job id. */
	typedef string job_id;
	
	/* A job status string supplied by the reporting service. No more than
		200 characters. 
	*/
	typedef string job_status;
	
	/* A job description string supplied by the reporting service. No more than
		1000 characters. 
	*/
	typedef string job_description;
	
	/* The amount of progress the job has made since the last update, summed
		to the total progress so far. */
	typedef int progress;
	
	/* The total progress of a job. */
	typedef int total_progress;
	
	/* The maximum possible progress of a job. */
	typedef int max_progress;
	
	/* The type of progress that is being tracked. One of:
		'none' - no numerical progress tracking
		'task' - Task based tracking, e.g. 3/24
		'percent' - percentage based tracking, e.g. 5/100%
	*/ 
	typedef string progress_type;
	
	/* Initialization information for progress tracking. Currently 3 choices:
		
		progress_type ptype - one of 'none', 'percent', or 'task'
		max_progress max- required only for task based tracking. The 
			total number of tasks until the job is complete.
	*/
	typedef structure {
		progress_type ptype;
		max_progress max;
	} InitProgress;
	
	
	/* A pointer to job results. All arguments are optional. Applications
		should use the default shock and workspace urls if omitted.
		list<string> shocknodes - the shocknode(s) where the results can be
			found.
		string shockurl - the url of the shock service where the data was
			saved.
		list<string> workspaceids - the workspace ids where the results can be
			found.
		string workspaceurl - the url of the workspace service where the data
			was saved.
	*/
	typedef structure {
		list<string> shocknodes;
		string shockurl;
		list<string> workspaceids;
		string workspaceurl;
	} Results;
		
	/* Create a new job status report. */
	funcdef create_job() returns(job_id job);
	
	/* Start a job and specify the job parameters. */
	funcdef start_job(job_id job, service_name service, job_status status, 
		job_description desc, InitProgress progress) returns();
	
	/* Create and start a job. */
	funcdef create_and_start_job(service_name service, job_status status, 
		job_description desc, InitProgress progress) returns(job_id job);
	
	/* Update the status and progress for a job. */
	funcdef update_job_progress(job_id job, job_status status, progress prog)
		returns();
		
	/* Update the status for a job. */
	funcdef update_job(job_id job, job_status status) returns();
	
	/* Get the description of a job. */
	funcdef get_job_description(job_id job) returns(service_name service,
		progress_type ptype, max_progress max, job_description desc);
	
	/* Get the status of a job. 
		If the progress type is 'none' total_progress will always be 0.
	*/
	funcdef get_job_status(job_id job) returns(timestamp last_update, 
		job_status status, total_progress progress, boolean complete,
		boolean error);
	
	/* Complete the job. After the job is completed, total_progress always
		equals max_progress.
	*/
	funcdef complete_job(job_id job, job_status status, boolean error,
		Results res) returns();
		
	/* Get the job results. */
	funcdef get_results(job_id job) returns(Results res);
	
	/* List service names. */
	funcdef get_services() returns (list<service_name> services);
	
	/* Information about a job. Note calls returning this structure will
		probably be slower than the more targeted calls.
	*/
	typedef tuple<job_id job, service_name service, job_status status,
		timestamp last_update, total_progress prog, max_progress max,
		progress_type ptype, boolean complete, boolean error,
		job_description desc, Results res> job_info;
	
	/* Get information about a job. */
	funcdef get_job_info(job_id job) returns(job_info info);

	/* Options for list_jobs command. 
		
		boolean oldest_first - return jobs with an ascending sort based on the
			creation date.
		int limit - limit the results to X jobs.
		int offset - skip the first X jobs.
		boolean completed - true to return only completed jobs, false to
			return only incomplete jobs.
		boolean error_only - true to return only jobs that errored out. 
			Overrides the completed option.
	*/
	typedef structure {
		boolean oldest_first;
		int limit;
		int offset;
		boolean completed;
		boolean error_only;
	} ListJobsOptions;
	
	/* List jobs. */
	funcdef list_jobs(service_name service, ListJobsOptions options)
		returns(list<job_info> jobs);
	
	/* Delete a job. Will error out if the job is not complete. */
	funcdef delete_job(job_id job) returns();
	
	/* Force delete a job - will always succeed, regardless of job state. */
	funcdef force_delete_job(job_id job) returns();
};