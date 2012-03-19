package com.hae.pipe;

public class Test extends Stage {
	public static void main(String[] args) throws Exception {
		Pipe.register("test", Test.class);
		Pipe pipe = new Pipe();
		assertEquals(0, pipe.run("literal 1234512345| split not string /4/ | cons | zzzcheck /4/4/"));
		System.out.println("Done");
	}
	public int execute(String s) throws PipeException {
		callpipe("(end ?) *: | l: locate /a/ | *.OUTPUT.1: ? l: | *.OUTPUT.0:");
		return 0;
	}
	public static void assertEquals(int i, int j) {
		if (i != j)
			System.out.println();
	}
}
