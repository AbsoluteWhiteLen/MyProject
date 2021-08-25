package pack_and_encrypt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//base class
public class BaseClass {

	// logger
	public static Logger logger = LogManager.getLogger();

	protected void error(int errorCode) {
		logger.error("error code is " + errorCode);
	}
}
