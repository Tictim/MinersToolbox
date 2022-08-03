package test;

import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import tictim.minerstoolbox.config.ProgressiveMiningConfig;
import tictim.minerstoolbox.config.ProgressiveMiningRule.Index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static tictim.minerstoolbox.MinersToolboxMod.LOGGER;
import static tictim.minerstoolbox.MinersToolboxMod.MODID;

@GameTestHolder(MODID)
public class ProgressiveMiningTest{
	static{
		GlobalTestReporter.replaceWith(new VerboseLogReporter());
	}

	private static final ProgressiveMiningConfig cfg = new ProgressiveMiningConfig();

	@PrefixGameTestTemplate(false)
	@GameTest(template = "empty")
	public static void test1(GameTestHelper helper){
		cfg.clear();
		load("t1");
		cfg.printAllInvalidReasons(LOGGER);
		assertValid(helper, "t1", 1, 2);
		helper.succeed();
	}

	@PrefixGameTestTemplate(false)
	@GameTest(template = "empty")
	public static void test2(GameTestHelper helper){
		cfg.clear();
		load("t2");
		cfg.printAllInvalidReasons(LOGGER);
		assertValid(helper, "t2", 1, 2, 3, 4, 5, 6, 7, 8);
		helper.succeed();
	}

	@PrefixGameTestTemplate(false)
	@GameTest(template = "empty")
	public static void invalid1(GameTestHelper helper){
		cfg.clear();
		load("invalid1");
		assertInvalid(helper, "invalid1", 1, 2, 3, 4, 5, 6);
		helper.succeed();
	}

	@PrefixGameTestTemplate(false)
	@GameTest(template = "empty")
	public static void ex1(GameTestHelper helper){
		cfg.clear();
		load("ex1");
		if(cfg.getRules().isEmpty()) helper.succeed();
		else helper.fail("No failure");
	}

	private static void load(String fileName){
		load(fileName, Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("progressive_mining_test/"+fileName+".json"));
	}
	private static void load(String fileName, InputStream in){
		try(in){
			try(var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){
				cfg.load(fileName, ProgressiveMiningConfig.LENIENT_GSON.fromJson(r, JsonObject.class), false);
			}
		}catch(IOException ex){
			throw new RuntimeException("Cannot read file \""+fileName+"\"", ex);
		}
		cfg.updateAndValidate();
	}

	private static void assertValid(GameTestHelper helper, String fileName, int... index){
		for(int i : index) assertValid(helper, new Index(fileName, i), true);
	}
	private static void assertInvalid(GameTestHelper helper, String fileName, int... index){
		for(int i : index) assertValid(helper, new Index(fileName, i), false);
	}
	private static void assertValid(GameTestHelper helper, Index index, boolean expected){
		switch(cfg.getRuleValidity(index)){
			case VALID -> {
				if(!expected) helper.fail("Rule "+index+" is not invalid");
			}
			case INVALID -> {
				if(expected) helper.fail("Rule "+index+" is not valid");
			}
			default -> helper.fail("Rule "+index+" is not found");
		}
	}
}
