/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.5                                                               *
 * Copyright (C) 1998-2009  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * License: BSD                                                            *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package jflex;

/**
 * HiLowEmitter
 * 
 * @author Gerwin Klein
 * @version $Revision$, $Date$
 */
public class HiLowEmitter extends PackEmitter {

  /** number of entries in expanded array */
  private int numEntries;

  /**
   * Create new emitter for values in [0, 0xFFFFFFFF] using hi/low encoding.
   * 
   * @param name   the name of the generated array
   */
  public HiLowEmitter(String name) {
    super(name);
  }

  /**
   * Emits hi/low pair unpacking code for the generated array. 
   * 
   * @see jflex.PackEmitter#emitUnpack()
   */
  public void emitUnpack() {
    // close last string chunk:
    println("\";");
    nl();
    println("  "+Options.lang.method_header(false, false, false, true, Options.lang.array_type(Options.lang.int_type()), "zzUnpack"+name, "()",null)+" {");
    println("    "+Options.lang.local(false, Options.lang.array_type(Options.lang.int_type()), "result", Options.lang.new_array(Options.lang.int_type(), ""+numEntries))+";");
    println("    "+Options.lang.local(true, Options.lang.int_type(), "offset", "0")+";");

    for (int i = 0; i < chunks; i++) {
      println("    offset = zzUnpack"+name+"("+constName()+"_PACKED_"+i+", offset, result);");
    }

    println("    return result;");
    println("  }");
    nl();

    println("  "+Options.lang.method_header(false, false, false, true, Options.lang.int_type(), "zzUnpack"+name, 
        "("+Options.lang.formal(false, "String", "packed") +","+
        Options.lang.formal(false, Options.lang.int_type(), "offset")+","+
        Options.lang.formal(false, Options.lang.array_type(Options.lang.int_type()), "result")+")",null)+" {");
    println("    "+Options.lang.local(true, Options.lang.int_type(), "i", "0")+";       /* index in packed string  */");
    println("    "+Options.lang.local(true, Options.lang.int_type(), "j", "offset")+";  /* index in unpacked array */");
    println("    "+Options.lang.local(false, Options.lang.int_type(), "l", "packed.length()")+";");
    println("    while (i < l) {");
    println("      "+Options.lang.local(false, Options.lang.int_type(), "high", "packed.charAt(i) << 16")+"; i+= 1");
    println("      "+Options.lang.array_index("result", "j")+" = high | packed.charAt(i); i+= 1; j += 1;");
    println("    }");
    println("    return j;");
    println("  }");
    super.emitUnpack();
  }

  /**
   * Emit one value using two characters. 
   *
   * @param val  the value to emit
   * @prec  0 <= val <= 0xFFFFFFFF 
   */
  public void emit(int val) {
    numEntries+= 1;
    breaks();
    emitUC(val >> 16);
    emitUC(val & 0xFFFF);        
  }
}
