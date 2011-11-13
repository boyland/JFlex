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
 * An emitter for an array encoded as count/value pairs in a string.
 * 
 * @author Gerwin Klein
 * @version $Revision$, $Date$
 */
public class CountEmitter extends PackEmitter {
  /** number of entries in expanded array */
  private int numEntries;
  
  /** translate all values by this amount */ 
  private int translate = 0;


  /**
   * Create a count/value emitter for a specific field.
   * 
   * @param name   name of the generated array
   */
  protected CountEmitter(String name) {
    super(name);
  }

  /**
   * Emits count/value unpacking code for the generated array. 
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
        "("+Options.lang.formal(false, "String", "packed") + "," +
        Options.lang.formal(false, Options.lang.int_type(), "offset")+","+
        Options.lang.formal(false, Options.lang.array_type(Options.lang.int_type()), "result")+")",null)+" {");
    println("    "+Options.lang.local(true, Options.lang.int_type(), "i", "0")+";       /* index in packed string  */");
    println("    "+Options.lang.local(true, Options.lang.int_type(), "j", "offset")+";  /* index in unpacked array */");
    println("    "+Options.lang.local(false, Options.lang.int_type(), "l", "packed.length()")+";");
    println("    while (i < l) {");
    println("      "+Options.lang.local(true, Options.lang.int_type(), "count", "packed.charAt(i)")+"; i+= 1");
    println("      "+Options.lang.local(true, Options.lang.int_type(), "value", "packed.charAt(i);")+" i+= 1");
    if (translate != 0) {
      println("      value-= "+translate);
    }
    println("      do { "+Options.lang.array_index("result", "j")+" = value; j+=1; count -= 1; } while (count > 0);");
    println("    }");
    println("    return j;");
    println("  }");
    emitUnpackCall();
  }

  /**
   * Translate all values by given amount.
   * 
   * Use to move value interval from [0, 0xFFFF] to something different.
   * 
   * @param i   amount the value will be translated by. 
   *            Example: <code>i = 1</code> allows values in [-1, 0xFFFE].
   */
  public void setValTranslation(int i) {
    this.translate = i;    
  }

  /**
   * Emit one count/value pair. 
   * 
   * Automatically translates value by the <code>translate</code> value. 
   * 
   * @param count
   * @param value
   * 
   * @see CountEmitter#setValTranslation(int)
   */
  public void emit(int count, int value) {
    numEntries+= count;
    breaks();
    emitUC(count);
    emitUC(value+translate);        
  }
}
