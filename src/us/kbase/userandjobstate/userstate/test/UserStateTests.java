package us.kbase.userandjobstate.userstate.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.userandjobstate.test.UserJobStateTestCommon;
import us.kbase.userandjobstate.userstate.UserState;
import us.kbase.userandjobstate.userstate.exceptions.NoSuchKeyException;

import com.mongodb.BasicDBObject;

public class UserStateTests {
	
	private static UserState us;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		UserJobStateTestCommon.destroyAndSetupDB();
		
		String host = UserJobStateTestCommon.getHost();
		String mUser = UserJobStateTestCommon.getMongoUser();
		String mPwd = UserJobStateTestCommon.getMongoPwd();
		String db = UserJobStateTestCommon.getDB();
		
		if (mUser != null) {
			us = new UserState(host, db, "userstate", mUser, mPwd);
		} else {
			us = new UserState(host, db, "userstate");
		}
	}
	
	private static String long101;
	static {
		for (int i = 0; i < 10; i++) {
			long101 += "aaaaaaaaaabbbbbbbbbb";
		}
		long101 += "f";
	}
	
	@Test
	public void illegalArgs() throws Exception {
		checkBadArgs(null, "foo", true, "key", "user cannot be null or the empty string");
		checkBadArgs(null, "foo", false, "key", "user cannot be null or the empty string");
		checkBadArgs("", "foo", true, "key", "user cannot be null or the empty string");
		checkBadArgs("", "foo", false, "key", "user cannot be null or the empty string");
		checkBadArgs(long101, "foo", false, "key", "user exceeds the maximum length of 100");
		checkBadArgs("u", null, true, "key", "service cannot be null or the empty string");
		checkBadArgs("u", null, false, "key", "service cannot be null or the empty string");
		checkBadArgs("u", "", true, "key", "service cannot be null or the empty string");
		checkBadArgs("u", "", false, "key", "service cannot be null or the empty string");
		checkBadArgs("u", long101, false, "key", "service exceeds the maximum length of 100");
		checkBadArgs("u", "afa)aafe", true, "key", "Illegal character in service name afa)aafe: )");
		checkBadArgs("u", "afae-afa", false, "key", "Illegal character in service name afae-afa: -");
		checkBadArgs("u", "foo", true, null, "key cannot be null or the empty string");
		checkBadArgs("u", "foo", false, null, "key cannot be null or the empty string");
		checkBadArgs("u", "foo", true, "", "key cannot be null or the empty string");
		checkBadArgs("u", "foo", false, "", "key cannot be null or the empty string");
		checkBadArgs("u", "foo", false, long101, "key exceeds the maximum length of 100");
		checkListStateBadArgs(null, "foo", true, "user cannot be null or the empty string");
		checkListStateBadArgs(null, "foo", false, "user cannot be null or the empty string");
		checkListStateBadArgs("", "foo", true, "user cannot be null or the empty string");
		checkListStateBadArgs("", "foo", false, "user cannot be null or the empty string");
		checkListStateBadArgs("u", null, true, "service cannot be null or the empty string");
		checkListStateBadArgs("u", null, false, "service cannot be null or the empty string");
		checkListStateBadArgs("u", "", true, "service cannot be null or the empty string");
		checkListStateBadArgs("u", "", false, "service cannot be null or the empty string");
		checkListStateBadArgs("u", "afa)aafe", true, "Illegal character in service name afa)aafe: )");
		checkListStateBadArgs("u", "afae-afa", false, "Illegal character in service name afae-afa: -");
		checkListServicesBadArgs(null, true, "user cannot be null or the empty string");
		checkListServicesBadArgs(null, false, "user cannot be null or the empty string");
		checkListServicesBadArgs("", true, "user cannot be null or the empty string");
		checkListServicesBadArgs("", false, "user cannot be null or the empty string");
	}
	
	private void checkListStateBadArgs(String user, String service, boolean auth,
			String exception) throws Exception {
		try {
			us.listState(user, service, auth);
			fail("list state with bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private void checkListServicesBadArgs(String user, boolean auth,
			String exception) throws Exception {
		try {
			us.listServices(user, auth);
			fail("list services with bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
	}
	
	private void checkBadArgs(String user, String service, boolean auth,
			String key, String exception) throws Exception {
		Object data = "foo";
		try {
			us.setState(user, service, auth, key, data);
			fail("set state w/ bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
		try {
			us.getState(user, service, auth, key);
			fail("get state w/ bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
		try {
			us.removeState(user, service, auth, key);
			fail("remove state w/ bad args");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is(exception));
		}
	}
	
	@Test
	public void getNonExistantState() throws Exception {
		us.setState("u", "nostate", false, "thing1", "foo");
		us.setState("u", "nostate", true, "thing1", "foo");
		try {
			us.getState("u", "nostate", false, "thing");
			fail("got non-existant state");
		} catch (NoSuchKeyException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("There is no key thing for the unauthorized service nostate"));
		}
		try {
			us.getState("u", "nostate", true, "thing");
			fail("got non-existant state");
		} catch (NoSuchKeyException nske) {
			assertThat("correct exception", nske.getLocalizedMessage(),
					is("There is no key thing for the authorized service nostate"));
		}
	}
	
	@Test
	public void removeState() throws Exception {
		us.setState("u", "remstate", false, "thing1", "foo");
		us.setState("u", "remstate", true, "thing1", "foo");
		assertThat("get works prior to remove",
				us.getState("u", "remstate", false, "thing1"),
				is((Object) "foo"));
		assertThat("get works prior to remove",
				us.getState("u", "remstate", true, "thing1"),
				is((Object) "foo"));
		us.removeState("u", "remstate", false, "thing1");
		assertThat("get works after unauthed remove",
				us.getState("u", "remstate", true, "thing1"),
				is((Object) "foo"));
		try {
			us.getState("u", "remstate", false, "thing1");
			fail("got removed state");
		} catch (NoSuchKeyException nske) {
			assertThat("correct exception", nske.getLocalizedMessage(),
					is("There is no key thing1 for the unauthorized service remstate"));
		}
		us.setState("u", "remstate", false, "thing1", "foo");
		us.removeState("u", "remstate", true, "thing1");
		assertThat("get works after authed remove",
				us.getState("u", "remstate", false, "thing1"),
				is((Object) "foo"));
		try {
			us.getState("u", "remstate", true, "thing1");
			fail("got removed state");
		} catch (NoSuchKeyException nske) {
			assertThat("correct exception", nske.getLocalizedMessage(),
					is("There is no key thing1 for the authorized service remstate"));
		}
	}

	@Test
	public void getSetListStateAndService() throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bar", "baz");
		us.setState("foo", "serv1", false, "key1", data);
		assertThat("got correct data back",
				us.getState("foo", "serv1", false, "key1"),
				is((Object) new BasicDBObject(data)));
		Map<String, Object> data2 = new HashMap<String, Object>();
		data2.put("bar", data);
		us.setState("foo", "authserv1", true, "authkey1", data2);
		assertThat("got correct data back",
				us.getState("foo", "authserv1", true, "authkey1"),
				is((Object) new BasicDBObject(data2)));
		us.setState("foo", "serv1", false, "key2", data);
		us.setState("foo", "serv2", false, "key", data);
		us.setState("foo", "authserv1", true, "authkey2", data2);
		us.setState("foo", "authserv2", true, "authkey", data2);

		checkListState("foo", "serv1", false, Arrays.asList("key1", "key2"));
		checkListState("foo", "serv2", false, Arrays.asList("key"));
		checkListState("foo", "serv3", false, new ArrayList<String>());
		checkListState("foo", "authserv1", true, Arrays.asList("authkey1", "authkey2"));
		checkListState("foo", "authserv2", true, Arrays.asList("authkey"));
		checkListState("foo", "authserv3", true, new ArrayList<String>());
		
		checkListServ("foo", false, Arrays.asList("serv1", "serv2"));
		checkListServ("foo1", false, new ArrayList<String>());
		checkListServ("foo", true, Arrays.asList("authserv1", "authserv2"));
		checkListServ("foo1", true, new ArrayList<String>());
		
		us.setState("foo", "nullserv", false, "null", null);
		us.setState("foo", "nullserv", true, "null", null);
		assertThat("got correct data back",
				us.getState("foo", "nullserv", true, "null"),
				is((Object) null));
		assertThat("got correct data back",
				us.getState("foo", "nullserv", false, "null"),
				is((Object) null));
	}
	
	private void checkListServ(String user, boolean auth,
			List<String> expected) throws Exception {
		Set<String> expc = new HashSet<String>(expected);
		assertThat("get correct services", us.listServices(user, auth),
				is(expc));
	}
	
	private void checkListState(String user, String service, boolean auth,
			List<String> expected) throws Exception {
		Set<String> expc = new HashSet<String>(expected);
		assertThat("get correct keys", us.listState(user, service, auth),
				is(expc));
	}
	
	@Test
	public void setUnserializable() throws Exception {
		try {
			us.setState("foo", "unserializable", false, "a", new JFrame());
			fail("saved unserializable object");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("Unable to serialize value"));
		}
	}
	
	private static final String BIG_STR = 
			"afbiekdiekaotkjaialeotialeicitlaielfialefotaleofleif" +
			"afbiekdiekaotkjaialeotialeicitlaielfialefotaleofleif";
	@Test
	public void failOnBigState() throws Exception {
		Map<Integer, String> data = new HashMap<Integer, String>();
		for (int i = 0; i < 10000; i++) {
			data.put(i, BIG_STR);
		}
		try {
			us.setState("foo", "servBig", false, "key1", data);
			fail("set state with way too big data");
		} catch (IllegalArgumentException iae) {
			assertThat("correct exception", iae.getLocalizedMessage(),
					is("Value cannot be > 1000000 bytes when serialized"));
		}
	}
}
