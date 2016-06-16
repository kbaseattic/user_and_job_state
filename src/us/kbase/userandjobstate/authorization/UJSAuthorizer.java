package us.kbase.userandjobstate.authorization;

import java.util.List;

import us.kbase.userandjobstate.authorization.exceptions.UJSAuthorizationException;
import us.kbase.userandjobstate.jobstate.Job;

/** An abstract implementation of a UJS authorization strategy. The default
 * implementation is defined in this class, and the class can be extended
 * to add new strategies.
 * @author gaprice@lbl.gov
 *
 */
public abstract class UJSAuthorizer {
	
	/** The default authorization strategy ('DEFAULT') for the UJS, which uses
	 * the ACLs stored in the UJS for authorization.
	 */
	public static final AuthorizationStrategy DEFAULT_AUTH_STRAT = 
			new AuthorizationStrategy("DEFAULT");
	
	/** The default authorization parameter for the UJS. This is actually
	 * unused but cannot be null or the empty string and so is defined to be
	 * 'DEFAULT'.
	 */
	public static final String DEFAULT_AUTH_PARAM = "DEFAULT";

	private void checkAuthParam(final String authParam) {
		if (authParam == null || authParam.isEmpty()) {
			throw new IllegalArgumentException(
					"authParam cannot be null or empty");
		}
	}
	
	private void checkUser(final String user) {
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException(
					"user cannot be null or empty");
		}
	}
	
	/** Authorize creation of a job.
	 * @param strat the authorization strategy to use.
	 * @param authParam the authorization parameter.
	 * @throws UJSAuthorizationException if authorization is denied.
	 */
	public void authorizeCreate(
			final AuthorizationStrategy strat,
			final String authParam)
			throws UJSAuthorizationException {
		checkAuthParam(authParam);
		if (strat.equals(DEFAULT_AUTH_STRAT)) {
			// authorized, since anyone can create a job. Do nothing.
		} else {
			externallyAuthorizeCreate(strat, authParam);
		}
	}
	
	/** Authorize creation of a job using a non-default authorization source.
	 * @param strat the authorization strategy to use.
	 * @param authParam the authorization parameter.
	 * @throws UJSAuthorizationException if authorization is denied.
	 */
	protected abstract void externallyAuthorizeCreate(
			final AuthorizationStrategy strat,
			final String authParam)
			throws UJSAuthorizationException;

	/** Authorize reading a job.
	 * @param strat the authorization strategy to use.
	 * @param user the user requesting authorization.
	 * @param authParam the authorization parameter.
	 * @param j the job requiring authorization.
	 * @throws UJSAuthorizationException if authorization is denied.
	 */
	public void authorizeRead(
			final AuthorizationStrategy strat,
			final String user,
			final String authParam,
			final Job j)
			throws UJSAuthorizationException {
		checkAuthParam(authParam);
		checkUser(user);
		if (j == null) {
			throw new NullPointerException("job cannot be null");
		}
		if (strat.equals(DEFAULT_AUTH_STRAT)) {
			if (!user.equals(j.getUser()) && !j.getShared().contains(user)) {
				throw new UJSAuthorizationException(String.format(
						"Job %s is not viewable by user %s", j.getID(), user));
			}
		} else {
			externallyAuthorizeRead(strat, user, authParam, j);
		}
	}
	
	/** Authorize reading a job using a non-default authorization source.
	 * @param strat the authorization strategy to use.
	 * @param user the user requesting authorization.
	 * @param authParam the authorization parameter.
	 * @param j the job requiring authorization.
	 * @throws UJSAuthorizationException if authorization is denied.
	 */
	protected abstract void externallyAuthorizeRead(
			final AuthorizationStrategy strat,
			final String user,
			final String authParam,
			final Job j)
			throws UJSAuthorizationException;

	/** Authorize reading a set of jobs associated with one or more
	 * authorization parameters.
	 * @param strat the authorization strategy to use.
	 * @param user the user requesting authorization.
	 * @param authParams the set of authorization parameters.
	 * @throws UJSAuthorizationException if authorization is denied.
	 */
	public void authorizeRead(
			final AuthorizationStrategy strat,
			final String user,
			final List<String> authParams)
			throws UJSAuthorizationException {
		checkUser(user);
		if (authParams == null || authParams.isEmpty()) {
			throw new IllegalArgumentException(
					"authParams cannot be null or empty");
		}
		for (final String p: authParams) {
			checkAuthParam(p);
		}
		if (strat.equals(DEFAULT_AUTH_STRAT)) {
			/* do nothing. A user can read ujs jobs shared with him/her.
			 * however, the job fetching logic in JobState needs to pull back
			 * the right jobs.
			 */
		} else {
			externallyAuthorizeRead(strat, user, authParams);
		}
	}
	
	/** Authorize reading a set of jobs associated with one or more
	 * authorization parameters using a non-default authorization source.
	 * @param strat the authorization strategy to use.
	 * @param user the user requesting authorization.
	 * @param authParams the set of authorization parameters.
	 * @throws UJSAuthorizationException if authorization is denied.
	 */
	protected abstract void externallyAuthorizeRead(
			final AuthorizationStrategy strat,
			final String user,
			final List<String> authParams)
			throws UJSAuthorizationException;

}