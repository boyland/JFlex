/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * JFlex 1.4.3                                                             *
 * Copyright (C) 1998-2009  Gerwin Klein <lsf@jflex.de>                    *
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

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * This class manages the actual code generation, putting
 * the scanner together, filling in skeleton sections etc.
 *
 * Table compression, String packing etc. is also done here.
 *
 * @author Gerwin Klein
 * @version JFlex 1.4.3, $Revision$, $Date$
 */
final public class Emitter {
    
  // bit masks for state attributes
  static final private int FINAL = 1;
  static final private int NOLOOK = 8;

  static final private String date = (new SimpleDateFormat()).format(new Date());

  private File inputFile;

  private PrintWriter out;
  private Skeleton skel;
  private LexScan scanner;
  private LexParse parser;
  private DFA dfa;

  // for switch statement:
  // table[i][j] is the set of input characters that leads from state i to state j
  private CharSet table[][];

  private boolean isTransition[];
  
  // noTarget[i] is the set of input characters that have no target state in state i
  private CharSet noTarget[];
      
  // for row killing:
  private int numRows;
  private int [] rowMap;
  private boolean [] rowKilled;
  
  // for col killing:
  private int numCols;
  private int [] colMap;
  private boolean [] colKilled;
  

  /** maps actions to their switch label */
  private Hashtable actionTable = new Hashtable();

  private CharClassInterval [] intervals;

  private String visibility = "public";

  public Emitter(File inputFile, LexParse parser, DFA dfa) throws IOException {

    String name = getBaseName(parser.scanner.className) + "." + Options.lang.extension();

    File outputFile = normalize(name, inputFile);

    Out.println("Writing code to \""+outputFile+"\"");
    
    this.out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
    this.parser = parser;
    this.scanner = parser.scanner;
    this.visibility = scanner.visibility;
    this.inputFile = inputFile;
    this.dfa = dfa;
    this.skel = new Skeleton(out);
  }

  /**
   * Computes base name of the class name. Needs to take into account generics.
   *
   * @see LexScan#className
   * @return the
   */
  public static String getBaseName(String className) {
    int gen = className.indexOf('<'); //XXX Needs to be generalized to work with Scala
    if (gen < 0) {
      return className;
    }
    else {
      return className.substring(0, gen);
    }
  }
  

  /**
   * Constructs a file in Options.getDir() or in the same directory as
   * another file. Makes a backup if the file already exists.
   *
   * @param name  the name (without path) of the file
   * @param path  the path where to construct the file
   * @param input fall back location if path = <tt>null</tt>
   *              (expected to be a file in the directory to write to)   
   */
  public static File normalize(String name, File input) {
    File outputFile;

    if ( Options.getDir() == null ) 
      if ( input == null || input.getParent() == null )
        outputFile = new File(name);
      else
        outputFile = new File(input.getParent(), name);
    else 
      outputFile = new File(Options.getDir(), name);
        
    if ( outputFile.exists() && !Options.no_backup ) {      
      File backup = new File( outputFile.toString()+"~" );
      
      if ( backup.exists() ) backup.delete();
      
      if ( outputFile.renameTo( backup ) )
        Out.println("Old file \""+outputFile+"\" saved as \""+backup+"\"");
      else
        Out.println("Couldn't save old file \""+outputFile+"\", overwriting!");
    }

    return outputFile;
  }
  
  private void println() {
    out.println();
  }

  private void println(String line) {
    out.println(line);
  }

  private void println(int i) {
    out.println(i);
  }

  private void print(String line) {
    out.print(line);
  }

  private void print(int i) {
    out.print(i);
  }

  private void print(int i, int tab) {
    int exp;

    if (i < 0) 
      exp = 1;
    else
      exp = 10;

    while (tab-- > 1) {
      if (Math.abs(i) < exp) print(" ");
      exp*= 10;
    }

    print(i);
  }

  private boolean hasGenLookAhead() {
    return dfa.lookaheadUsed;
  }
  
  private void emitLookBuffer() {
    if (!hasGenLookAhead()) return;
    
    println("  /** For the backwards DFA of general lookahead statements */");
    // println("  private boolean [] zzFin = new boolean [ZZ_BUFFERSIZE+1];");
    println("  " + Options.lang.field(false, false, false, 
        Options.lang.array_type(Options.lang.boolean_type()), 
        "zzFin", Options.lang.new_array(Options.lang.boolean_type(),
            "ZZ_BUFFERSIZE+1")));
    println();
  }
  
  private void emitScanError() {
    //print("  private void zzScanError(int errorCode)");
    print("  " + Options.lang.method_header(false, false, true, false, Options.lang.void_type(), 
        "zzScanError", "(" + Options.lang.formal(false, Options.lang.int_type(), "errorCode")+")", null));
    
    if (scanner.scanErrorException != null) 
      print(" throws "+scanner.scanErrorException);

    println(" {");

    skel.emitNext();

    if (scanner.scanErrorException == null)
      println("    throw new Error(message);");
    else
      println("    throw new "+scanner.scanErrorException+"(message);");    

    skel.emitNext();

    // print("  "+visibility+" void yypushback(int number) "); 
    print("  "+Options.lang.method_header(false, visibility.equals("public"), true, false, 
          Options.lang.void_type(), "yypushback", 
          "(" + Options.lang.formal(false, Options.lang.int_type(), "number") + ")", null));
    
    if (scanner.scanErrorException == null)
      println(" {");
    else       
      println(" throws "+scanner.scanErrorException+" {");
  }

  private void emitMain() {
    if ( !(scanner.standalone || scanner.debugOption || scanner.cupDebug) ) return;
    // XXX: Not ported to Scala

    if ( scanner.cupDebug ) {
      println("  /**");
      println("   * Converts an int token code into the name of the");
      println("   * token by reflection on the cup symbol class/interface "+scanner.cupSymbol);
      println("   *");
      println("   * This code was contributed by Karl Meissner <meissnersd@yahoo.com>"); 
      println("   */");
      println("  private String getTokenName(int token) {");
      println("    try {");
      println("      java.lang.reflect.Field [] classFields = " + scanner.cupSymbol + ".class.getFields();");
      println("      for (int i = 0; i < classFields.length; i++) {");
      println("        if (classFields[i].getInt(null) == token) {");
      println("          return classFields[i].getName();");
      println("        }");
      println("      }");
      println("    } catch (Exception e) {");
      println("      e.printStackTrace(System.err);");
      println("    }");
      println("");
      println("    return \"UNKNOWN TOKEN\";");
      println("  }");
      println("");
      println("  /**");
      println("   * Same as "+scanner.functionName+" but also prints the token to standard out");
      println("   * for debugging.");
      println("   *");
      println("   * This code was contributed by Karl Meissner <meissnersd@yahoo.com>"); 
      println("   */");

      print("  "+visibility+" ");
      if ( scanner.tokenType == null ) {
        if ( scanner.isInteger )
          print( "int" );
        else 
          if ( scanner.isIntWrap )
            print( "Integer" );
          else
            print( "Yytoken" );
      }
      else
        print( scanner.tokenType );
      
      print(" debug_");
      
      print(scanner.functionName);
      
      print("() throws java.io.IOException");
    
      if ( scanner.lexThrow != null ) {
        print(", ");
        print(scanner.lexThrow);
      }

      if ( scanner.scanErrorException != null ) {
        print(", ");
        print(scanner.scanErrorException);
      }
      
      println(" {");

      println("    java_cup.runtime.Symbol s = "+scanner.functionName+"();");
      print("    System.out.println( ");
      if (scanner.lineCount) print("\"line:\" + (yyline+1) + ");
      if (scanner.columnCount) print("\" col:\" + (yycolumn+1) + ");
      println("\" --\"+ yytext() + \"--\" + getTokenName(s.sym) + \"--\");");
      println("    return s;");
      println("  }");
      println("");
    }

    if ( scanner.standalone ) {
      println("  /**");
      println("   * Runs the scanner on input files.");
      println("   *");
      println("   * This is a standalone scanner, it will print any unmatched");
      println("   * text to System.out unchanged.");      
      println("   *");
      println("   * @param argv   the command line, contains the filenames to run");
      println("   *               the scanner on.");
      println("   */");
    }
    else {
      println("  /**");
      println("   * Runs the scanner on input files.");
      println("   *");
      println("   * This main method is the debugging routine for the scanner.");
      println("   * It prints debugging information about each returned token to");
      println("   * System.out until the end of file is reached, or an error occured.");
      println("   *"); 
      println("   * @param argv   the command line, contains the filenames to run");
      println("   *               the scanner on."); 
      println("   */"); 
    }      
    
    String className = getBaseName(scanner.className);
    
    println("  public static void main(String argv[]) {");
    println("    if (argv.length == 0) {");
    println("      System.out.println(\"Usage : java "+className+" <inputfile>\");");
    println("    }");
    println("    else {");
    println("      for (int i = 0; i < argv.length; i++) {");
    println("        "+className+" scanner = null;");
    println("        try {");
    println("          scanner = new "+className+"( new java.io.FileReader(argv[i]) );");

    if ( scanner.standalone ) {      
      println("          while ( !scanner.zzAtEOF ) scanner."+scanner.functionName+"();");
    }
    else if (scanner.cupDebug ) {
      println("          while ( !scanner.zzAtEOF ) scanner.debug_"+scanner.functionName+"();");
    }
    else {
      println("          do {");
      println("            System.out.println(scanner."+scanner.functionName+"());");
      println("          } while (!scanner.zzAtEOF);");
      println("");
    }
 
    println("        }");
    println("        catch (java.io.FileNotFoundException e) {");
    println("          System.out.println(\"File not found : \\\"\"+argv[i]+\"\\\"\");");
    println("        }");
    println("        catch (java.io.IOException e) {");
    println("          System.out.println(\"IO error scanning file \\\"\"+argv[i]+\"\\\"\");");
    println("          System.out.println(e);");
    println("        }"); 
    println("        catch (Exception e) {");
    println("          System.out.println(\"Unexpected exception:\");");
    println("          e.printStackTrace();");
    println("        }"); 
    println("      }");
    println("    }");
    println("  }");
    println("");    
  }
  
  private void emitNoMatch() {
    println("            zzScanError(ZZ_NO_MATCH);");
  }
  
  private void emitNextInput() {
    println("          if (zzCurrentPosL < zzEndReadL) {");
    println("            zzInput = " + Options.lang.array_index("zzBufferL","zzCurrentPosL")+";");
    println("            zzCurrentPosL += 1;");
    println("          } else if (zzAtEOF) {");
    println("            zzInput = YYEOF;");
    println("            " + Options.lang.break_block("zzForAction")+";");
    println("          }");
    println("          else {");
    println("            // store back cached positions");
    println("            zzCurrentPos  = zzCurrentPosL;");
    println("            zzMarkedPos   = zzMarkedPosL;");
    println("            " + Options.lang.local(false, Options.lang.boolean_type(), "eof", "zzRefill()") + ";");
    println("            // get translated positions and possibly new buffer");
    println("            zzCurrentPosL  = zzCurrentPos;");
    println("            zzMarkedPosL   = zzMarkedPos;");
    println("            zzBufferL      = zzBuffer;");
    println("            zzEndReadL     = zzEndRead;");
    println("            if (eof) {");
    println("              zzInput = YYEOF;");
    println("              " + Options.lang.break_block("zzForAction")+";"); 
    println("            }");
    println("            else {");
    println("              zzInput = " + Options.lang.array_index("zzBufferL","zzCurrentPosL")+";");
    println("              zzCurrentPosL += 1;");
    println("            }");
    println("          }"); 
  }

  private void emitHeader() {
    println("/* The following code was generated by JFlex "+Main.version+" on "+date+" */");   
    println(""); 
  } 

  private void emitUserCode() {
    if ( scanner.userCode.length() > 0 )
      println(scanner.userCode.toString());
  }

  private void emitClassName() {    
    if (!endsWithJavadoc(scanner.userCode)) {
      String path = inputFile.toString();
      // slashify path (avoid backslash u sequence = unicode escape)
      if (File.separatorChar != '/') {
	    path = path.replace(File.separatorChar, '/');
      }
    	
      println("/**");
      println(" * This class is a scanner generated by ");
      println(" * <a href=\"http://www.jflex.de/\">JFlex</a> "+Main.version);
      println(" * on "+date+" from the specification file");    
      println(" * <tt>"+path+"</tt>");      
      println(" */");
    }   

    if ( scanner.isPublic ) print("public ");

    if ( scanner.isAbstract) print("abstract ");
   
    if ( scanner.isFinal ) print("final ");
    
    print("class ");
    print(scanner.className);
    
    if (Options.lang == Language.SCALA) {
      print("(private var zzReader : java.io.Reader");
      emitCtorArgs();
      print(")");
    }
    
    if ( scanner.isExtending != null ) {
      print(" extends ");
      print(scanner.isExtending);
    }

    if ( scanner.isImplementing != null ) {
      //XXX: Workaround bad way %extends is written:
      if (Options.lang == Language.SCALA)
        print(" extends ");
      else
        print(" implements ");
      print(scanner.isImplementing);
    }   
    
    println(" {");
    
    if (Options.lang == Language.SCALA) {
      print("  def this(in : java.io.InputStream) = ");    

      print("this(new java.io.InputStreamReader(in)");

      emitCtorActuals();      
      
      println(");");
    }
  }  

  /**
   * Try to find out if user code ends with a javadoc comment 
   * 
   * @param buffer   the user code
   * @return true    if it ends with a javadoc comment
   */
  public static boolean endsWithJavadoc(StringBuffer usercode) {
    String s = usercode.toString().trim();
        
    if (!s.endsWith("*/")) return false;
    
    // find beginning of javadoc comment   
    int i = s.lastIndexOf("/**");    
    if (i < 0) return false; 
       
    // javadoc comment shouldn't contain a comment end
    return s.substring(i,s.length()-2).indexOf("*/") < 0;
  }


  private void emitLexicalStates() {
    Enumeration stateNames = scanner.states.names();
    
    while ( stateNames.hasMoreElements() ) {
      String name = (String) stateNames.nextElement();
      
      int num = scanner.states.getNumber(name).intValue();

      // println("  "+visibility+" static final int "+name+" = "+2*num+";");
      println("  "+Options.lang.field(visibility.equals("public"), true, false, Options.lang.int_type(), name, ""+(2*num)) + ";");
    }

    // can't quite get rid of the indirection, even for non-bol lex states: 
    // their DFA states might be the same, but their EOF actions might be different
    // (see bug #1540228)
    println("");
    println("  /**");
    println("   * ZZ_LEXSTATE[l] is the state in the DFA for the lexical state l");
    println("   * ZZ_LEXSTATE[l+1] is the state in the DFA for the lexical state l");
    println("   *                  at the beginning of a line");
    println("   * l is of the form l = 2*k, k a non negative integer");
    println("   */");
    //println("  private static final int ZZ_LEXSTATE[] = { ");
    println("  " + Options.lang.field(false, true, false, Options.lang.array_type(Options.lang.int_type()), 
                                      "ZZ_LEXSTATE", Options.lang.array_literal_start(Options.lang.int_type())));
  
    int i, j = 0;
    print("    ");
    
    for (i = 0; i < 2*dfa.numLexStates-1; i++) {
      print( dfa.entryState[i], 2 );

      print(", ");

      if (++j >= 16) {
        println();
        print("    ");
        j = 0;
      }
    }
            
    println( dfa.entryState[i] );
    println("  "+Options.lang.array_literal_stop()+";");
  }

  private void emitDynamicInit() {    
    int count = 0;
    int value = dfa.table[0][0];

    println("  /** ");
    println("   * The transition table of the DFA");
    println("   */");

    CountEmitter e = new CountEmitter("Trans");
    e.setValTranslation(+1); // allow vals in [-1, 0xFFFE]
    e.emitInit();
    
    for (int i = 0; i < dfa.numStates; i++) {
      if ( !rowKilled[i] ) {
        for (int c = 0; c < dfa.numInput; c++) {
          if ( !colKilled[c] ) {
            if (dfa.table[i][c] == value) {
              count++;
            } 
            else {
              e.emit(count, value);

              count = 1;
              value = dfa.table[i][c];              
            }
          }
        }
      }
    }

    e.emit(count, value);
    e.emitUnpack();
    
    println(e.toString());
  }


  private void emitCharMapInitFunction() {

    CharClasses cl = parser.getCharClasses();
    
    if ( cl.getMaxCharCode() < 256 ) return;

    println("");
    println("  /** ");
    println("   * Unpacks the compressed character translation table.");
    println("   *");
    println("   * @param packed   the packed character translation table");
    println("   * @return         the unpacked character translation table");
    println("   */");
    // println("  private static char [] zzUnpackCMap(String packed) {");
    println("  " + Options.lang.method_header(false, false, true, true, 
        Options.lang.array_type(Options.lang.char_type()), "zzUnpackCMap", 
        "("+Options.lang.formal(false, "String", "packed") + ")", null) + "{");
    // println("    char [] map = new char[0x10000];");
    println("    " + Options.lang.local(false, Options.lang.array_type(Options.lang.char_type()), "map", 
        Options.lang.new_array(Options.lang.char_type(), "0x10000")));
    println("    "+Options.lang.local(true, Options.lang.int_type(), "i", "0")+";  /* index in packed string  */");
    println("    "+Options.lang.local(true, Options.lang.int_type(), "j", "0")+";  /* index in unpacked array */");
    println("    while (i < "+2*intervals.length+") {");
    println("      "+Options.lang.local(true, Options.lang.int_type(), "count", "packed.charAt(i)")+"; i+= 1");
    println("      "+Options.lang.local(true, Options.lang.char_type(), "value", "packed.charAt(i);")+" i+= 1");
    //println("      int  count = packed.charAt(i); i+=1");
    //println("      char value = packed.charAt(i); i+=1");
    println("      do { "+Options.lang.array_index("map","j")+" = value; j+=1; count-=1; } while (count > 0);");
    println("    }");
    println("    return map;");
    println("  }");
  }

  private void emitZZTrans() {    

    int i,c;
    int n = 0;
    
    println("  /** ");
    println("   * The transition table of the DFA");
    println("   */");
    // println("  private static final int ZZ_TRANS [] = {"); 
    println("  " + Options.lang.field(false, true, false, Options.lang.array_type(Options.lang.int_type()), 
        "ZZ_TRANS", Options.lang.array_literal_start(Options.lang.int_type())));

    print("    ");
    for (i = 0; i < dfa.numStates; i++) {
      
      if ( !rowKilled[i] ) {        
        for (c = 0; c < dfa.numInput; c++) {          
          if ( !colKilled[c] ) {            
            if (n >= 10) {
              println();
              print("    ");
              n = 0;
            }
            print( dfa.table[i][c] );
            if (i != dfa.numStates-1 || c != dfa.numInput-1)
              print( ", ");
            n++;
          }
        }
      }
    }

    println();
    println("  "+ Options.lang.array_literal_stop()+";");
  }
  
  private void emitCharMapArrayUnPacked() {
  
    CharClasses cl = parser.getCharClasses();
    
    println("");
    println("  /** ");
    println("   * Translates characters to character classes");
    println("   */");
    // println("  private static final char [] ZZ_CMAP = {");
    println("  " + Options.lang.field(false, true, false, Options.lang.array_type(Options.lang.char_type()), 
        "ZZ_CMAP", Options.lang.array_literal_start(Options.lang.char_type())));

    int n = 0;  // numbers of entries in current line    
    print("    ");
    
    int max =  cl.getMaxCharCode();
	
    // not very efficient, but good enough for <= 255 characters
    for (char c = 0; c <= max; c++) {
      print(colMap[cl.getClassCode(c)],2);
      
      if (c < max) {
        print(", ");        
        if ( ++n >= 16 ) { 
          println();
          print("    ");
          n = 0; 
        }
      }
    }
    
    println();
    println("  "+ Options.lang.array_literal_stop()+";");
    println();
  }

  private void emitCharMapArray() {       
    CharClasses cl = parser.getCharClasses();

    if ( cl.getMaxCharCode() < 256 ) {
      emitCharMapArrayUnPacked();
      return;
    }

    // ignores cl.getMaxCharCode(), emits all intervals instead

    intervals = cl.getIntervals();
    
    println("");
    println("  /** ");
    println("   * Translates characters to character classes");
    println("   */");
    // println("  private static final String ZZ_CMAP_PACKED = ");
    println("  " + Options.lang.field(false, true, false, "String", "ZZ_CMAP_PACKED", ""));
  
    int n = 0;  // numbers of entries in current line    
    print("    \"");
    
    int i = 0;
    int count, value;
    while ( i < intervals.length ) {
      count = intervals[i].end-intervals[i].start+1;
      value = colMap[intervals[i].charClass];

      // count could be >= 0x10000
      while (count > 0xFFFF) {
        printUC(0xFFFF);
        printUC(value);
        count -= 0xFFFF;
        n++;       
      }
        
      printUC(count);
      printUC(value);

      if (i < intervals.length-1) {
        if ( ++n >= 10 ) { 
          println("\"+");
          print("    \"");
          n = 0;
        }
      }

      i++;
    }
      
    println("\";");
    println();

    println("  /** ");
    println("   * Translates characters to character classes");
    println("   */");
    println("  " + Options.lang.field(false, true, false, Options.lang.array_type(Options.lang.char_type()), 
        "ZZ_CMAP", "zzUnpackCMap(ZZ_CMAP_PACKED)") + ";");
    println();
  }


  /**
   * Print number as octal/unicode escaped string character.
   * 
   * @param c   the value to print
   * @prec  0 <= c <= 0xFFFF 
   */
  private void printUC(int c) {
    if (c > 255) {
      out.print("\\u");
      if (c < 0x1000) out.print("0");
      out.print(Integer.toHexString(c));
    }
    else {
      out.print("\\");
      out.print(Integer.toOctalString(c));
    }    
  }


  private void emitRowMapArray() {
    println("");
    println("  /** ");
    println("   * Translates a state to a row index in the transition table");
    println("   */");
    
    HiLowEmitter e = new HiLowEmitter("RowMap");
    e.emitInit();
    for (int i = 0; i < dfa.numStates; i++) {
      e.emit(rowMap[i]*numCols);
    }    
    e.emitUnpack();
    println(e.toString());
  }


  private void emitAttributes() {
    println("  /**");
    println("   * ZZ_ATTRIBUTE[aState] contains the attributes of state <code>aState</code>");
    println("   */");
    
    CountEmitter e = new CountEmitter("Attribute");    
    e.emitInit();
    
    int count = 1;
    int value = 0; 
    if ( dfa.isFinal[0]    ) value = FINAL;
    if ( !isTransition[0]  ) value|= NOLOOK;
       
    for (int i = 1;  i < dfa.numStates; i++) {      
      int attribute = 0;      
      if ( dfa.isFinal[i]    ) attribute = FINAL;
      if ( !isTransition[i]  ) attribute|= NOLOOK;

      if (value == attribute) {
        count++;
      }
      else {        
        e.emit(count, value);
        count = 1;
        value = attribute;
      }
    }
    
    e.emit(count, value);    
    e.emitUnpack();
    
    println(e.toString());
  }


  private void emitClassCode() {
    if ( scanner.eofCode != null ) {
      println("  /** denotes if the user-EOF-code has already been executed */");
      //println("  private boolean zzEOFDone;");
      println("  "+Options.lang.field(false, false, true, Options.lang.boolean_type(), "zzEOFDone", "false"));
      println("");
    }
    
    if ( scanner.classCode != null ) {
      println("  /* user code: */");
      println(scanner.classCode);
    }
  }

  private void emitConstructorDecl() {
    if (Options.lang == Language.SCALA) return;
    emitConstructorDecl(true);
    
    if ((scanner.standalone || scanner.debugOption) && 
        scanner.ctorArgs.size() > 0) {
      Out.warning(ErrorMessages.get(ErrorMessages.CTOR_DEBUG));
      println();
      emitConstructorDecl(false);
    }
  }
  
  private void emitConstructorDecl(boolean printCtorArgs) {
 
    String warn = 
        "// WARNING: this is a default constructor for " +
        "debug/standalone only. Has no custom parameters or init code.";
    
    if (!printCtorArgs) println(warn); 
    
    print("  ");

    if ( scanner.isPublic ) print("public ");   
    print( getBaseName(scanner.className) );      
    print("(java.io.Reader in");
    if (printCtorArgs) emitCtorArgs();
    print(")");
    
    if ( scanner.initThrow != null && printCtorArgs) {
      print(" throws ");
      print( scanner.initThrow );
    }
    
    println(" {");

    if ( scanner.initCode != null && printCtorArgs) {
      print("  ");
      print( scanner.initCode );
    }

    println("    this.zzReader = in;");

    println("  }");
    println();

    
    println("  /**");
    println("   * Creates a new scanner.");
    println("   * There is also java.io.Reader version of this constructor.");
    println("   *");
    println("   * @param   in  the java.io.Inputstream to read input from.");
    println("   */");
    if (!printCtorArgs) println(warn);
    
    print("  ");
    if ( scanner.isPublic ) print("public ");    
    print( getBaseName(scanner.className) );      
    print("(java.io.InputStream in");
    if (printCtorArgs) emitCtorArgs();
    print(")");
    
    if ( scanner.initThrow != null && printCtorArgs ) {
      print(" throws ");
      print( scanner.initThrow );
    }
    
    println(" {");    

    print("    this(new java.io.InputStreamReader(in)");
    if (printCtorArgs) {
      emitCtorActuals();      
    }
    println(");");
    
    println("  }");
  }

  private void emitCtorActuals() {
    for (int i=0; i < scanner.ctorArgs.size(); i++) {
      print(", "+scanner.ctorArgs.elementAt(i));
    }
  }

  private void emitCtorArgs() {
    for (int i = 0; i < scanner.ctorArgs.size(); i++) {
      print(", "+Options.lang.formal(false, scanner.ctorTypes.elementAt(i).toString(),scanner.ctorArgs.elementAt(i).toString()));
    }    
  }
  
  private void emitDoEOF() {
    if ( scanner.eofCode == null ) return;
    
    println("  /**");
    println("   * Contains user EOF-code, which will be executed exactly once,");
    println("   * when the end of file is reached");
    println("   */");
    
    // print("  private void zzDoEOF()");
    print("  " + Options.lang.method_header(false, false, false, false, Options.lang.void_type(), "zzDoEOF", "()", null));
    
    if ( scanner.eofThrow != null ) {
      print(" throws ");
      print(scanner.eofThrow);
    }
    
    println(" {");
    
    println("    if (!zzEOFDone) {");
    println("      zzEOFDone = true;");
    println("    "+scanner.eofCode );
    println("    }");
    println("  }");
    println("");
    println("");
  }

  private void emitLexFunctHeader() {
    boolean visible = scanner.cupCompatible || visibility.equals("public");
    String rtype = scanner.tokenType == null ? (scanner.isInteger ? Options.lang.int_type() : scanner.isIntWrap ? "Integer" : "Yytoken") :
      scanner.tokenType.toString();
    String excs = "java.io.IOException";
    boolean isOverriding = false; // (Options.lang == Language.SCALA);
    
    if ( scanner.lexThrow != null ) {
      excs = excs + ", " + scanner.lexThrow;
    }

    if ( scanner.scanErrorException != null ) {
      excs = excs + ", " + scanner.scanErrorException;
    }
    
    println("  " + Options.lang.method_header(isOverriding, visible, true, false, rtype, scanner.functionName, "()",excs));
    println(" {");
    
    skel.emitNext();

    if ( scanner.useRowMap ) {
      //println("    int [] zzTransL = ZZ_TRANS;");
      //println("    int [] zzRowMapL = ZZ_ROWMAP;");
      //println("    int [] zzAttrL = ZZ_ATTRIBUTE;");
      println("    " + Options.lang.local(false, Options.lang.array_type(Options.lang.int_type()), "zzTransL", "ZZ_TRANS")+";");
      println("    " + Options.lang.local(false, Options.lang.array_type(Options.lang.int_type()), "zzRowMapL", "ZZ_ROWMAP")+";");
      println("    " + Options.lang.local(false, Options.lang.array_type(Options.lang.int_type()), "zzAttrL", "ZZ_ATTRIBUTE")+";");
    }

    skel.emitNext();    
        
    if ( scanner.charCount ) {
      println("      yychar+= zzMarkedPosL-zzStartRead;");
      println("");
    }
    
    if ( scanner.lineCount || scanner.columnCount ) {
      println("      " + Options.lang.local(true, Options.lang.boolean_type(), "zzR", "false")+";");
      println("      zzCurrentPosL = zzStartRead");
      println("      while (zzCurrentPosL < zzMarkedPosL) {");
      println("        "+ Options.lang.switch_header(Options.lang.array_index("zzBufferL","zzCurrentPosL")) + "{");
      println("        "+Options.lang.start_case("'\\u000B'")); 
      println("        "+Options.lang.add_case("'\\u000C'")); 
      println("        "+Options.lang.add_case("'\\u0085'"));
      println("        "+Options.lang.add_case("'\\u2028'")); 
      println("        "+Options.lang.add_case("'\\u2029'") + Options.lang.start_case_body());
      if ( scanner.lineCount )
        println("          yyline+=1;");
      if ( scanner.columnCount )
        println("          yycolumn = 0;");
      println("          zzR = false;");
      println("          "+Options.lang.end_case_body());      
      println("        "+Options.lang.start_case("'\\r'" + Options.lang.start_case_body()));
      if ( scanner.lineCount )
        println("          yyline+=1;");
      if ( scanner.columnCount )
        println("          yycolumn = 0;");
      println("          zzR = true;");
      println("          "+Options.lang.end_case_body());
      println("        "+Options.lang.start_case("'\\n'" + Options.lang.start_case_body()));
      println("          if (zzR)");
      println("            zzR = false;");
      println("          else {");
      if ( scanner.lineCount )
        println("            yyline+=1;");
      if ( scanner.columnCount )
        println("            yycolumn = 0;");
      println("          }");
      println("          "+Options.lang.end_case_body());
      println("        "+Options.lang.gen_default()+ Options.lang.start_case_body());
      println("          zzR = false;");
      if ( scanner.columnCount ) 
        println("          yycolumn+=1;");
      println("          " + Options.lang.end_case_body());
      println("        }");
      println("        zzCurrentPosL += 1;");
      println("      }");
      println();

      if ( scanner.lineCount ) {
        println("      if (zzR) {");
        println("        // peek one character ahead if it is \\n (if we have counted one line too much)");
        println("        "+Options.lang.local(true, Options.lang.boolean_type(), "zzPeek", "false")+";");
        println("        if (zzMarkedPosL < zzEndReadL)");
        println("          zzPeek = "+Options.lang.array_index("zzBufferL","zzMarkedPosL")+" == '\\n';");
        println("        else if (zzAtEOF)");
        println("          zzPeek = false;");
        println("        else {");
        println("          "+Options.lang.local(false, Options.lang.boolean_type(), "eof", "zzRefill()")+";");
        println("          zzEndReadL = zzEndRead;");
        println("          zzMarkedPosL = zzMarkedPos;");
        println("          zzBufferL = zzBuffer;");
        println("          if (eof) ");
        println("            zzPeek = false;");
        println("          else ");
        println("            zzPeek = "+Options.lang.array_index("zzBufferL","zzMarkedPosL")+" == '\\n';");
        println("        }");
        println("        if (zzPeek) yyline-= 1;");
        println("      }");
      }
    }

    if ( scanner.bolUsed ) {
      // zzMarkedPos > zzStartRead <=> last match was not empty
      // if match was empty, last value of zzAtBOL can be used
      // zzStartRead is always >= 0
      println("      if (zzMarkedPosL > zzStartRead) {");
      println("        "+ Options.lang.switch_header(Options.lang.array_index("zzBufferL","zzMarkedPosL-1")));
      println("        "+Options.lang.start_case("'\\n'")); 
      println("        "+Options.lang.add_case("'\\u000B'")); 
      println("        "+Options.lang.add_case("'\\u000C'")); 
      println("        "+Options.lang.add_case("'\\u0085'"));
      println("        "+Options.lang.add_case("'\\u2028'")); 
      println("        "+Options.lang.add_case("'\\u2029'") + Options.lang.start_case_body());
      println("          zzAtBOL = true;");
      println("          "+Options.lang.end_case_body()); 
      println("        "+Options.lang.start_case("'\\r'")+Options.lang.start_case_body()); 
      println("          if (zzMarkedPosL < zzEndReadL)");
      println("            zzAtBOL = "+Options.lang.array_index("zzBufferL","zzMarkedPosL")+" != '\\n';");
      println("          else if (zzAtEOF)");
      println("            zzAtBOL = false;");
      println("          else {");
      println("            "+Options.lang.local(false, Options.lang.boolean_type(), "eof", "zzRefill()")+";");
      println("            zzMarkedPosL = zzMarkedPos;");
      println("            zzEndReadL = zzEndRead;");
      println("            zzBufferL = zzBuffer;");
      println("            if (eof) ");
      println("              zzAtBOL = false;");
      println("            else ");
      println("              zzAtBOL = "+Options.lang.array_index("zzBufferL","zzMarkedPosL")+" != '\\n';");
      println("          }");      
      println("          "+Options.lang.end_case_body()); 
      println("        "+Options.lang.gen_default()+Options.lang.start_case_body()); 
      println("          zzAtBOL = false;");
      println("        "+Options.lang.end_case_body()); 
      println("        }");
      println("      }"); 
    }

    skel.emitNext();
    
    if (scanner.bolUsed) {
      println("      if (zzAtBOL)");
      println("        zzState = "+Options.lang.array_index("ZZ_LEXSTATE","zzLexicalState+1")+";");
      println("      else");    
      println("        zzState = "+Options.lang.array_index("ZZ_LEXSTATE","zzLexicalState")+";");
      println();
    }
    else {
      println("      zzState = "+Options.lang.array_index("ZZ_LEXSTATE","zzLexicalState")+";");
      println();
    }

    skel.emitNext();
  }

  
  private void emitGetRowMapNext() {
    println("          "+Options.lang.local(false, Options.lang.int_type(), "zzNext", 
        Options.lang.array_index("zzTransL",
            Options.lang.array_index("zzRowMapL","zzState")+" + "+
            Options.lang.array_index("zzCMapL","zzInput")))+";");
    println("          if (zzNext == "+DFA.NO_TARGET+") "+Options.lang.break_block("zzForAction")+";");
    println("          zzState = zzNext;");
    println();

    println("          "+Options.lang.local(false, Options.lang.int_type(), "zzAttributes", Options.lang.array_index("zzAttrL","zzState"))+";");

    println("          if ( (zzAttributes & "+FINAL+") == "+FINAL+" ) {");

    skel.emitNext();
    
    println("            if ( (zzAttributes & "+NOLOOK+") == "+NOLOOK+" ) "+Options.lang.break_block("zzForAction")+";");

    skel.emitNext();    
  }  

  private void emitTransitionTable() {
    transformTransitionTable();
    
    println("          zzInput = "+Options.lang.array_index("zzCMapL","zzInput")+";");
    println();

    println("          "+Options.lang.local(true, Options.lang.boolean_type(), "zzIsFinal", "false")+";");
    println("          "+Options.lang.local(true, Options.lang.boolean_type(), "zzNoLookAhead","false")+";");
    println();
    
    println("          "+Options.lang.start_label_block("zzForNext")+Options.lang.switch_header("zzState")+" {");

    for (int state = 0; state < dfa.numStates; state++)
      if (isTransition[state]) emitState(state);

    println("            "+Options.lang.gen_default()+Options.lang.start_case_body());
    println("              // if this is ever reached, there is a serious bug in JFlex");
    println("              zzScanError(ZZ_UNKNOWN_ERROR);");
    println("              "+Options.lang.end_case_body());
    println("          } " + Options.lang.end_label_block("zzForNext"));
    println();
    
    println("          if ( zzIsFinal ) {");
    
    skel.emitNext();
    
    println("            if ( zzNoLookAhead ) "+Options.lang.break_block("zzForAction")+";");

    skel.emitNext();    
  }


  /**
   * Escapes all " ' \ tabs and newlines
   */
  private String escapify(String s) {
    StringBuffer result = new StringBuffer(s.length()*2);
    
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '\'': result.append("\\\'"); break;
      case '\"': result.append("\\\""); break;
      case '\\': result.append("\\\\"); break;
      case '\t': result.append("\\t"); break;
      case '\r': if (i+1 == s.length() || s.charAt(i+1) != '\n') result.append("\"+ZZ_NL+\""); 
                 break;
      case '\n': result.append("\"+ZZ_NL+\""); break;
      default: result.append(c);
      }
    }

    return result.toString();
  }

  public void emitActionTable() {
    int lastAction = 1;    
    int count = 0;
    int value = 0;

    println("  /** ");
    println("   * Translates DFA states to action switch labels.");
    println("   */");
    CountEmitter e = new CountEmitter("Action");    
    e.emitInit();

    for (int i = 0; i < dfa.numStates; i++) {
      int newVal = 0; 
      if ( dfa.isFinal[i] ) {
        Action action = dfa.action[i];
        if (action.isEmittable()) {
          Integer stored = (Integer) actionTable.get(action);
          if ( stored == null ) { 
            stored = new Integer(lastAction++);
            actionTable.put(action, stored);
          }
          newVal = stored.intValue();
        }
      }
      
      if (value == newVal) {
        count++;
      }
      else {
        if (count > 0) e.emit(count,value);
        count = 1;
        value = newVal;        
      }
    }
    
    if (count > 0) e.emit(count,value);

    e.emitUnpack();    
    println(e.toString());
  }

  private void emitActions() {
    println("      "+Options.lang.switch_header(Options.lang.conditional("zzAction < 0","zzAction",Options.lang.array_index("ZZ_ACTION","zzAction")))+" {");

    int i = actionTable.size()+1;  
    Enumeration actions = actionTable.keys();
    while ( actions.hasMoreElements() ) {
      Action action = (Action) actions.nextElement();
      int label = ((Integer) actionTable.get(action)).intValue();

      println("        "+Options.lang.start_case(""+label)+Options.lang.start_case_body()); 
      
      if (action.lookAhead() == Action.FIXED_BASE) {
        println("          // lookahead expression with fixed base length");
        println("          zzMarkedPos = zzStartRead + "+action.getLookLength()+";");        
      }
      
      if (action.lookAhead() == Action.FIXED_LOOK || 
          action.lookAhead() == Action.FINITE_CHOICE) {
        println("          // lookahead expression with fixed lookahead length");
        println("          yypushback("+action.getLookLength()+");");        
      }
      
      if (action.lookAhead() == Action.GENERAL_LOOK) {
        println("          // general lookahead, find correct zzMarkedPos");
        println("          { "+Options.lang.local(true, Options.lang.int_type(), "zzFState", ""+dfa.entryState[action.getEntryState()])+";");
        println("            "+Options.lang.local(true, Options.lang.int_type(), "zzFPos", "zzStartRead")+";");
        println("            if (zzFin.length <= zzBufferL.length) { zzFin = "+
            Options.lang.new_array(Options.lang.boolean_type(), "zzBufferL.length+1")+"; }");
        println("            "+Options.lang.local(false, Options.lang.array_type(Options.lang.boolean_type()), "zzFinL", "zzFin")+";");
        println("            while (zzFState != -1 && zzFPos < zzMarkedPos) {");
        println("              if (("+Options.lang.array_index("zzAttrL","zzFState")+" & 1) == 1) { "+Options.lang.array_index("zzFinL","zzFPos")+" = true; } ");
        println("              zzInput = "+Options.lang.array_index("zzBufferL","zzFPos")+";");
        println("              zzFPos += 1;");
        println("              zzFState = "+Options.lang.array_index("zzTransL",
            Options.lang.array_index("zzRowMapL","zzFState")+" + "+
            Options.lang.array_index("zzCMapL","zzInput"))+";");
        println("            }");
        println("            if (zzFState != -1 && ("+Options.lang.array_index("zzAttrL","zzFState")+" & 1) == 1) { "+Options.lang.array_index("zzFinL","zzFPos")+" = true; } ");
        println();                
        println("            zzFState = "+dfa.entryState[action.getEntryState()+1]+";");
        println("            zzFPos = zzMarkedPos;");
        println("            while (!"+Options.lang.array_index("zzFinL","zzFPos")+" || ("+Options.lang.array_index("zzAttrL","zzFState")+" & 1) != 1) {");
        println("              zzFPos -= 1;");
        println("              zzInput = "+Options.lang.array_index("zzBufferL","zzFPos")+";");
        println("              zzFState = "+Options.lang.array_index("zzTransL",
            Options.lang.array_index("zzRowMapL","zzFState")+" + "+
            Options.lang.array_index("zzCMapL","zzInput"))+";");
        println("            };");
        println("            zzMarkedPos = zzFPos;");
        println("          }");
      }
      
      if ( scanner.debugOption ) {
        print("          System.out.println(");
        if ( scanner.lineCount )
          print("\"line: \"+(yyline+1)+\" \"+");
        if ( scanner.columnCount )
          print("\"col: \"+(yycolumn+1)+\" \"+");
        println("\"match: --\"+yytext()+\"--\");");        
        print("          System.out.println(\""+Options.lang.array_index("action",""+action.priority)+" { ");
        print(escapify(action.content));
        println(" }\");");
      }
      
      println("          { "+action.content);
      println("          }");
      println("          " + Options.lang.end_case_body());
      println("        "+Options.lang.start_case("" + (i++))+Options.lang.start_case_body()+Options.lang.end_case_body()); 
    }
  }

  private void emitEOFVal() {
    EOFActions eofActions = parser.getEOFActions();

    if ( scanner.eofCode != null ) 
      println("            zzDoEOF();");
      
    if ( eofActions.numActions() > 0 ) {
      println("            "+Options.lang.switch_header("zzLexicalState")+" {");
      
      Enumeration stateNames = scanner.states.names();

      // record lex states already emitted:
      Hashtable used = new Hashtable();

      // pick a start value for break case labels. 
      // must be larger than any value of a lex state:
      int last = dfa.numStates;
      
      while ( stateNames.hasMoreElements() ) {
        String name = (String) stateNames.nextElement();
        int num = scanner.states.getNumber(name).intValue();
        Action action = eofActions.getAction(num);

        if (action != null) {
          println("            "+Options.lang.start_case(name)+Options.lang.start_case_body()+" {");
          if ( scanner.debugOption ) {
            print("              System.out.println(");
            if ( scanner.lineCount )
              print("\"line: \"+(yyline+1)+\" \"+");
            if ( scanner.columnCount )
              print("\"col: \"+(yycolumn+1)+\" \"+");
            println("\"match: <<EOF>>\");");        
            print("              System.out.println(\"action ["+action.priority+"] { ");
            print(escapify(action.content));
            println(" }\");");
          }
          println("              "+action.content);
          println("            }");
          println("            " + Options.lang.end_case_body());
          println("            "+Options.lang.start_case(""+(++last))+Options.lang.start_case_body()+Options.lang.end_case_body());
        }
      }
      
      println("            "+Options.lang.gen_default()+Options.lang.start_case_body());
    }

    Action defaultAction = eofActions.getDefault();

    if (defaultAction != null) {
      println("              {");
      if ( scanner.debugOption ) {
        print("                System.out.println(");
        if ( scanner.lineCount )
          print("\"line: \"+(yyline+1)+\" \"+");
        if ( scanner.columnCount )
          print("\"col: \"+(yycolumn+1)+\" \"+");
        println("\"match: <<EOF>>\");");        
        print("                System.out.println(\""+Options.lang.array_index("action ",""+defaultAction.priority)+" { ");
        print(escapify(defaultAction.content));
        println(" }\");");
      }
      println("                " + defaultAction.content);
      println("              }");
    }
    else if ( scanner.eofVal != null ) 
      println("              { " + scanner.eofVal + " }");
    else if ( scanner.isInteger ) {
      if ( scanner.tokenType != null ) {
        Out.error(ErrorMessages.INT_AND_TYPE);
        throw new GeneratorException();
      }
      println("            return YYEOF;");
    }
    else
      println("            return null;");

    if (eofActions.numActions() > 0)
      println("              " + Options.lang.end_case_body());
      println("            }");
  }
  
  private void emitState(int state) {
    
    println("            "+Options.lang.start_case(""+state)+Options.lang.start_case_body());
    println("              "+Options.lang.switch_header("zzInput")+" {");
   
    int defaultTransition = getDefaultTransition(state);
    
    for (int next = 0; next < dfa.numStates; next++) {
            
      if ( next != defaultTransition && table[state][next] != null ) {
        emitTransition(state, next);
      }
    }
    
    if ( defaultTransition != DFA.NO_TARGET && noTarget[state] != null ) {
      emitTransition(state, DFA.NO_TARGET);
    }
    
    emitDefaultTransition(state, defaultTransition);
    
    println("              } " + Options.lang.end_case_body());
    println("");
  }
  
  private void emitTransition(int state, int nextState) {

    CharSetEnumerator chars;
    
    if (nextState != DFA.NO_TARGET) 
      chars = table[state][nextState].characters();
    else 
      chars = noTarget[state].characters();
  
    print("                "+Options.lang.start_case(""+chars.nextElement()));
    
    while ( chars.hasMoreElements() ) {
      print("                "+Options.lang.add_case(""+chars.nextElement()));
    } 
    
    print("                  "+Options.lang.start_case_body());
    if ( nextState != DFA.NO_TARGET ) {
      if ( dfa.isFinal[nextState] )
        print("zzIsFinal = true; ");
        
      if ( !isTransition[nextState] )
        print("zzNoLookAhead = true; ");
        
      if ( nextState == state ) 
        println(Options.lang.break_block("zzForNext")+";");
      else
        println("zzState = "+nextState+"; "+Options.lang.break_block("zzForNext")+";");
    }
    else
      println(Options.lang.break_block("zzForAction")+";");
    println("                "+Options.lang.end_case_body());
  }
  
  private void emitDefaultTransition(int state, int nextState) {
    print("                "+Options.lang.gen_default()+Options.lang.start_case_body());
    
    if ( nextState != DFA.NO_TARGET ) {
      if ( dfa.isFinal[nextState] )
        print("zzIsFinal = true; ");
        
      if ( !isTransition[nextState] )
        print("zzNoLookAhead = true; ");
        
      if ( nextState == state ) 
        println(Options.lang.break_block("zzForNext")+";");
      else
        println("zzState = "+nextState+"; "+Options.lang.break_block("zzForNext")+";");
    }
    else
      println(Options.lang.break_block("zzForAction")+";");
    println("                "+Options.lang.end_case_body());
  }
  
  private int getDefaultTransition(int state) {
    int max = 0;
    
    for (int i = 0; i < dfa.numStates; i++) {
      if ( table[state][max] == null )
        max = i;
      else
      if ( table[state][i] != null && table[state][max].size() < table[state][i].size() )
        max = i;
    }
    
    if ( table[state][max] == null ) return DFA.NO_TARGET;
    if ( noTarget[state] == null ) return max;
    
    if ( table[state][max].size() < noTarget[state].size() ) 
      max = DFA.NO_TARGET;
    
    return max;
  }

  // for switch statement:
  private void transformTransitionTable() {
    
    int numInput = parser.getCharClasses().getNumClasses()+1;

    int i;    
    char j;
    
    table = new CharSet[dfa.numStates][dfa.numStates];
    noTarget = new CharSet[dfa.numStates];
    
    for (i = 0; i < dfa.numStates;  i++) 
      for (j = 0; j < dfa.numInput; j++) {

        int nextState = dfa.table[i][j];
        
        if ( nextState == DFA.NO_TARGET ) {
          if ( noTarget[i] == null ) 
            noTarget[i] = new CharSet(numInput, colMap[j]);
          else
            noTarget[i].add(colMap[j]);
        }
        else {
          if ( table[i][nextState] == null ) 
            table[i][nextState] = new CharSet(numInput, colMap[j]);
          else
            table[i][nextState].add(colMap[j]);
        }
      }
  }

  private void findActionStates() {
    isTransition = new boolean [dfa.numStates];
    
    for (int i = 0; i < dfa.numStates;  i++) {
      char j = 0;
      while ( !isTransition[i] && j < dfa.numInput )
        isTransition[i] = dfa.table[i][j++] != DFA.NO_TARGET;
    }
  }

  
  private void reduceColumns() {
    colMap = new int [dfa.numInput];
    colKilled = new boolean [dfa.numInput];

    int i,j,k;
    int translate = 0;
    boolean equal;

    numCols = dfa.numInput;

    for (i = 0; i < dfa.numInput; i++) {
      
      colMap[i] = i-translate;
      
      for (j = 0; j < i; j++) {
        
        // test for equality:
        k = -1;
        equal = true;        
        while (equal && ++k < dfa.numStates) 
          equal = dfa.table[k][i] == dfa.table[k][j];
        
        if (equal) {
          translate++;
          colMap[i] = colMap[j];
          colKilled[i] = true;
          numCols--;
          break;
        } // if
      } // for j
    } // for i
  }
  
  private void reduceRows() {
    rowMap = new int [dfa.numStates];
    rowKilled = new boolean [dfa.numStates];
    
    int i,j,k;
    int translate = 0;
    boolean equal;

    numRows = dfa.numStates;

    // i is the state to add to the new table
    for (i = 0; i < dfa.numStates; i++) {
      
      rowMap[i] = i-translate;
      
      // check if state i can be removed (i.e. already
      // exists in entries 0..i-1)
      for (j = 0; j < i; j++) {
        
        // test for equality:
        k = -1;
        equal = true;
        while (equal && ++k < dfa.numInput) 
          equal = dfa.table[i][k] == dfa.table[j][k];
        
        if (equal) {
          translate++;
          rowMap[i] = rowMap[j];
          rowKilled[i] = true;
          numRows--;
          break;
        } // if
      } // for j
    } // for i
    
  } 


  /**
   * Set up EOF code section according to scanner.eofcode 
   */
  private void setupEOFCode() {
    if (scanner.eofclose) {
      scanner.eofCode = LexScan.conc(scanner.eofCode, "  yyclose();");
      scanner.eofThrow = LexScan.concExc(scanner.eofThrow, "java.io.IOException");
    }    
  } 


  /**
   * Main Emitter method.  
   */
  public void emit() {    

    setupEOFCode();

    if (scanner.functionName == null) 
      scanner.functionName = "yylex";

    reduceColumns();
    findActionStates();

    emitHeader();
    emitUserCode();
    emitClassName();
    
    skel.emitNext();
    
    println("  "+Options.lang.field(false, true, false, Options.lang.int_type(), "ZZ_BUFFERSIZE"," "+scanner.bufferSize)+";");

    if (scanner.debugOption) {
      println("  "+Options.lang.field(false, true, false, "String", "ZZ_NL", "System.getProperty(\"line.separator\")")+";");
    }

    skel.emitNext();

    emitLexicalStates();
   
    emitCharMapArray();
    
    emitActionTable();
    
    if (scanner.useRowMap) {
     reduceRows();
    
      emitRowMapArray();

      if (scanner.packed)
        emitDynamicInit();
      else
        emitZZTrans();
    }
    
    skel.emitNext();
    
    if (scanner.useRowMap) 
      emitAttributes();    
    
    skel.emitNext();
    
    emitLookBuffer();
    
    emitClassCode();
    
    skel.emitNext();
    
    emitConstructorDecl();
        
    emitCharMapInitFunction();

    skel.emitNext();
    
    emitScanError();

    skel.emitNext();        

    emitDoEOF();
    
    skel.emitNext();
    
    emitLexFunctHeader();
    
    emitNextInput();

    if (scanner.useRowMap)
      emitGetRowMapNext();
    else
      emitTransitionTable();
        
    skel.emitNext();

    emitActions();
        
    skel.emitNext();

    emitEOFVal();
    
    skel.emitNext();
    
    emitNoMatch();

    skel.emitNext();
    
    emitMain();
    
    skel.emitNext();

    out.close();
  }

}
