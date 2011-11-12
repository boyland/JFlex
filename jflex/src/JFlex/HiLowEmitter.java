/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * jflex                                                         *
 * Copyright (C) 1998-2008  Gerwin Klein <lsf@jflex.de>                    *
 * All rights reserved.                                                    *
 *                                                                         *
 * This program is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License. See the file      *
 * COPYRIGHT for more information.                                         *
 *                                                                         *
 * This program is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License along *
 * with this program; if not, write to the Free Software Foundation, Inc., *
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA                 *
 *                                                                         *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package JFlex;

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
   * @see JFlex.PackEmitter#emitUnPack()
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
