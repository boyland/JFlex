package jflex;

public class ScalaLanguage implements Language {

  public String extension() {
    return "scala";
  }

  public String void_type() {
    return "Unit";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#int_type()
   */
  public String int_type() {
    return "Int";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#char_type()
   */
  public String char_type() {
    return "Char";
  }

  public String boolean_type() {
    return "Boolean";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#array_type(java.lang.String)
   */
  public String array_type(String ty) {
    return "Array[" + ty + "]";
  }

  public String field(boolean isPublic, boolean isStatic, boolean mutable,
      String type, String name, String initial) {
    String base = local(mutable,type,name,initial);
    if (!isPublic) return " private " + base;
    return base;
  }

  /* (non-Javadoc)
   * @see JFlex.Language#formal(boolean, java.lang.String, java.lang.String)
   */
  public String formal(boolean mutable, String type, String name) {
    if (mutable) throw new RuntimeException("Scala doesn't have mutable formals");
    StringBuilder sb = new StringBuilder();
    sb.append(name);
    sb.append(':');
    sb.append(type);
    return sb.toString();
  }

  public String method_header(boolean overriding, boolean isPublic, boolean overridable, boolean isStatic,
      String type, String name, String params, String excs) {
    StringBuilder sb = new StringBuilder();
    if (!isPublic) sb.append("private ");
    if (overriding) sb.append("override ");
    sb.append("def ");
    sb.append(name);
    sb.append(params);
    sb.append(" : " + type + " = ");
    return sb.toString();
  }

  /* (non-Javadoc)
   * @see JFlex.Language#local(boolean, java.lang.String, java.lang.String, java.lang.String)
   */
  public String local(boolean mutable, String type, String name, String initial) {
    StringBuilder sb = new StringBuilder();
    sb.append(mutable ? "var " : "val ");
    sb.append(name);
    sb.append(':');
    sb.append(type);
    sb.append(" = ");
    sb.append(initial);
    return sb.toString();
  }

  public String conditional(String cond, String ifTrue, String ifFalse) {
    return "if ( " + cond + " ) " + ifTrue + "; else " + ifFalse;
  }

  public String new_array(String type, String size) {
    return "new Array[" + type + "](" + size + ")";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#array_index(java.lang.String, java.lang.String)
   */
  public String array_index(String array, String index) {
    return array + "(" + index + ")";
  }

  public String array_literal_start(String type) {
    return "Array(";
  }

  public String array_literal_stop() {
    return ")";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#switch_header(java.lang.String)
   */
  public String switch_header(String expr) {
    return "(" + expr + ") match ";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#start_case(java.lang.String)
   */
  public String start_case(String val) {
    return "case " + val;
  }

  /* (non-Javadoc)
   * @see JFlex.Language#add_case(java.lang.String)
   */
  public String add_case(String val) {
    return "|" + val;
  }

  /* (non-Javadoc)
   * @see JFlex.Language#gen_default()
   */
  public String gen_default() {
    return "case _";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#start_case_body()
   */
  public String start_case_body() {
    return " => {";
  }

  /* (non-Javadoc)
   * @see JFlex.Language#end_case_body()
   */
  public String end_case_body() {
    return "}";
  }

  public String start_label_block(String name) {
    return "/*" + name + ":*/ try {";
  }
  public String break_block(String name) {
    return "throw ZZbreak(\"" + name + "\")";
  }
  public String end_label_block(String name) {
    return "} catch { case ZZbreak(\"" + name + "\") => () }";
  }

}
