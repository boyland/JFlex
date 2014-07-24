/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * jflex 1.4                                                               *
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

/*
 * This code contibuted originally by John Boyland.
 */
package jflex;

/**
 * Implement the language support for Java
 * @author boyland
 */
public class JavaLanguage implements Language {

  public String extension() {
    return "java";
  }
  
  public String void_type() {
    // TODO Auto-generated method stub
    return "void";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#int_type()
   */
  public String int_type() {
    return "int";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#char_type()
   */
  public String char_type() {
    return "char";
  }
  
  public String boolean_type() {
    return "boolean";
  }


  public String field(boolean isPublic, boolean isStatic, boolean mutable, String type,
      String name, String initial) {
    StringBuilder sb = new StringBuilder();
    sb.append(isPublic ? "public " : "private ");
    if (isStatic) sb.append("static ");
    if (!mutable) sb.append("final ");
    sb.append(type);
    sb.append(' ');
    sb.append(name);
    sb.append(" = ");
    sb.append(initial);
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see JFlex.Language#array_type(java.lang.String)
   */
  public String array_type(String ty) {
    return ty + " []";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#formal(boolean, java.lang.String, java.lang.String)
   */
  public String formal(boolean mutable, String type, String name) {
    StringBuilder sb = new StringBuilder();
    if (!mutable) sb.append("final ");
    sb.append(type);
    sb.append(' ');
    sb.append(name);
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see JFlex.Language#method_header(boolean, boolean, java.lang.String, java.lang.String, java.lang.String)
   */
  public String method_header(boolean overriding, boolean isPublic, boolean overridable, boolean isStatic,
      String type, String name, String params, String excs) {
    StringBuilder sb = new StringBuilder();
    if (overriding) sb.append("@Override\n  ");
    sb.append(isPublic ? "public " : "private ");
    if (isStatic) sb.append("static ");
    if (!overridable) sb.append("final ");
    sb.append(type);
    sb.append(' ');
    sb.append(name);
    sb.append(params); 
    if (excs != null) sb.append("throws " + excs);
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see JFlex.Language#local(boolean, java.lang.String, java.lang.String, java.lang.String)
   */
  public String local(boolean mutable, String type, String name, String initial) {
    return formal(mutable,type,name) + " = " + initial;
  }

  public String conditional(String cond, String ifTrue, String ifFalse) {
    return cond + " ? " + ifTrue + " : " + ifFalse;
  }

  public String new_array(String type, String size) {
    return "new " + type + "[" + size + "]";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#array_index(java.lang.String, java.lang.String)
   */
  public String array_index(String array, String index) {
    return array + "[" + index + "]";
  }

  public String array_literal_start(String type) {
    return "{";
  }

  public String array_literal_stop() {
    return "}";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#switch_header(java.lang.String)
   */
  public String switch_header(String expr) {
    return "switch (" + expr + ") ";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#start_case(java.lang.String)
   */
  public String start_case(String val) {
    return "case " + val + ":\n";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#add_case(java.lang.String)
   */
  public String add_case(String val) {
    return start_case(val);
  }

  /* (non-Javadoc)
   * @see JFlex.Language#gen_default()
   */
  public String gen_default() {
    return "default:\n";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#start_case_body()
   */
  public String start_case_body() {
    return "";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#end_case_body()
   */
  public String end_case_body() {
    return "break;";
  }

  public String end_case_body_unsafe() {
    return "// fall through";
  }

  public String start_label_block(String name) {
    return name + ": {";
  }
  public String break_block(String name) {
    return "break " + name;
  }
  public String end_label_block(String name) {
    return "}";
  }

}
