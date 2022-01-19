/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.5                                                               *
 * Copyright (C) 1998-2010  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * License: BSD                                                            *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package jflex;

import junit.framework.TestCase;

/**
 * PackEmitterTest
 * 
 * @author Gerwin Klein
 * @version $Revision$, $Date$
 */
public class PackEmitterTest extends TestCase {

  private PackEmitter p;


  /**
   * Constructor for PackEmitterTest.
   */
  public PackEmitterTest() {
    super("PackEmitter test");
  }

  public void setUp() {
    p = new PackEmitter("Bla") {
          public void emitUnpackImplem() { }
    };
  }

  public void testInit() {
    p.emitInit();
    assertEquals(
      Out.NL +
      "  private static final String ZZ_BLA_PACKED_0 = " + Out.NL +
      "    \"", 
      p.toString());
  }

  public void testUnpackCall() {
    p.emitUnpackCall();
    assertEquals(
      "  private static final int [] ZZ_BLA = zzUnpackBla();" + Out.NL,
      p.toString());
  }

  public void testEmitUCplain() {    
    p.emitUC(8);
    p.emitUC(0xFF00);
    
    assertEquals("\\10\\uff00", p.toString());
  }
  
  public void testLineBreak() {
    for (int i = 0; i < 36; i++) {
      p.breaks();
      p.emitUC(i);
    }
    System.out.println(p);
    assertEquals(
            "\\0\\1\\2\\3\\4\\5\\6\\7\\10\\11\\12\\13\\14\\15\\16\\17\"+"+Out.NL+
      "    \"\\20\\21\\22\\23\\24\\25\\26\\27\\30\\31\\32\\33\\34\\35\\36\\37\"+"+Out.NL+
      "    \"\\40\\41\\42\\43",
      p.toString());
  }
  
  public void testScalaUCplain() {
    try {
      Options.lang = Language.SCALA;
      for (int i = 0; i < 36; i++) {
        p.breaks();
        p.emitUC(i);
      }
    } finally {
      Options.lang = Language.JAVA;
    }
    System.out.println(p);
    assertEquals(
        "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007" +
        "\\u0008\\u0009\\n\\u000b\\u000c\\u000d\\u000e\\u000f" + "\"+" + Out.NL + "    " + '"' +
        "\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017" +
        "\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f" + "\"+" + Out.NL + "    " + '"' +
        " !\\\"#"
    , p.toString());
  }
}
