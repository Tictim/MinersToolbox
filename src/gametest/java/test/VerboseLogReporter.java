package test;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestTimeoutException;
import net.minecraft.gametest.framework.LogTestReporter;
import org.slf4j.Logger;

public class VerboseLogReporter extends LogTestReporter{
	private static final Logger LOGGER = LogUtils.getLogger();

	@Override public void onTestFailed(GameTestInfo info){
		if(info.getError() instanceof GameTestAssertException||
				info.getError() instanceof GameTestTimeoutException)
			super.onTestFailed(info);
		else if(info.isRequired())
			LOGGER.error("{} failed!", info.getTestName(), info.getError());
		else LOGGER.warn("(optional) {} failed.", info.getTestName(), info.getError());
	}
}
