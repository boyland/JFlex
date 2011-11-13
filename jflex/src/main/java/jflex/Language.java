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
 * Code generation routines for languages.
 * This abstracts some of the difference between Java and Scala.
 * It doesn't do everything.
 * 
 * @author John Boyland
 * @version $Revision$, $Date$
 */
public interface Language {
  String extension();
  
  // types
  String void_type();
  String int_type();  
  String char_type();  
  String boolean_type();
  String array_type(String ty);

  // variables
  String field(boolean isPublic, boolean isStatic, boolean mutable, String type, String name, String initial);
  String formal(boolean mutable, String type, String name);
  String method_header(boolean overriding, boolean isPublic, boolean overridable, boolean isStatic, String type,
		       String name, String params, String excs);
  String local(boolean mutable, String type, String name, String initial);

  // expressions
  String conditional(String cond, String ifTrue, String ifFalse);
  String new_array(String type, String size);
  String array_index(String array, String index);
  String array_literal_start(String type);
  String array_literal_stop();

  // pattern matching
  String switch_header(String expr);
  String start_case(String val);
  String add_case(String val);
  String start_case_body();
  String end_case_body();
  String gen_default();
  
  // Exiting loops;
  String start_label_block(String name);
  String break_block(String name);
  String end_label_block(String name);

  public static final Language JAVA = new JavaLanguage();
  public static final Language SCALA = new ScalaLanguage();
}
