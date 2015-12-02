package randoop.operation;

import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import randoop.ExecutionOutcome;
import randoop.Globals;
import randoop.NormalExecution;
import randoop.main.GenInputsAbstract;
import randoop.sequence.ExecutableSequence;
import randoop.sequence.Variable;
import randoop.util.PrimitiveTypes;
import randoop.util.Reflection;

/**
 * Immutable.
 * Represents a one-dimensional
 * array creation statement, e.g. "int[] x = new int[2] { 3, 7 };"
 */
public final class ArrayCreation implements Operation, Serializable {

  private static final long serialVersionUID = 20100429;

  /** ID for parsing purposes (see StatementKinds.parse method) */
  public static final String ID = "array";

  // State variables.
  private final int length;
  private final Class<?> elementType;

  // Cached values (for improved performance). Their values
  // are computed upon the first invocation of the respective
  // getter method.
  private List<Class<?>> inputTypesCached;
  private Class<?> outputType;
  private int hashCodeCached;
  private boolean hashCodeComputed= false;

  /**
   * @param elementType type of objects in the array
   * @param length number of objects allowed in the array
   */
  public ArrayCreation(Class<?> elementType, int length) {

    // Check legality of arguments.
    if (elementType == null) throw new IllegalArgumentException("elementType cannot be null.");
    if (length < 0) throw new IllegalArgumentException("arity cannot be less than zero: " + length);

    // Set state variables.
    this.elementType = elementType;
    this.length = length;
    this.outputType = Array.newInstance(elementType, 0).getClass();
  }

  private Object writeReplace() throws ObjectStreamException {
    return new SerializableArrayCreation(elementType, length);
  }

  /**
   * Returns the class of type of elements held in this ArrayDeclarationInfo
   */
  public Class<?> getElementType() {
    return this.elementType;
  }

  /**
   * Returns the length of elements held in this ArrayDeclarationInfo
   * */
  public int getLength() {
    return this.length;
  }

  /**
   * Extracts the input constraints for this ArrayDeclarationInfo
   * @return list of input constraints
   */
  public List<Class<?>> getInputTypes() {
    if (inputTypesCached == null) {
      this.inputTypesCached = new ArrayList<Class<?>>(length);
      for (int i = 0 ; i < length ; i++)
        inputTypesCached.add(elementType);
      inputTypesCached = Collections.unmodifiableList(inputTypesCached);
    }
    return Collections.unmodifiableList(this.inputTypesCached);
  }

  /**
   * Executes this statement, given the inputs to the statement. Returns
   * the results of execution as an ResultOrException object and can
   * output results to specified PrintStream.
   */
  public ExecutionOutcome execute(Object[] statementInput, PrintStream out) {
    if (statementInput.length > length)
      throw new IllegalArgumentException("Too many arguments:"
          + statementInput.length + " capacity:" + length);
    long startTime = System.currentTimeMillis();
    assert statementInput.length == this.length;
    Object theArray = Array.newInstance(this.elementType, this.length);
    for (int i = 0; i < statementInput.length; i++)
      Array.set(theArray, i, statementInput[i]);
    long totalTime = System.currentTimeMillis() - startTime;
    return new NormalExecution(theArray, totalTime);
  }

  @Override
  public String toString() {
    return "array_of_" + this.elementType.getSimpleName() + "_of_size_" + this.length;
  }

  public String toStringShort() {
    return toString();
  }

  public String toStringVerbose() {
    return toString();
  }

  /**
   * Returns constraint to represent new reference to this statement,
   * namely the receiver that is generated.
   */
  public Class<?> getOutputType() {
    return outputType;
  }

  /**
   * Appends string representation of ArrayDeclarationInfo into b.
   */
  public void appendCode(Variable newVar, List<Variable> inputVars, StringBuilder b) {
    if (inputVars.size() > length)
      throw new IllegalArgumentException("Too many arguments:"
          + inputVars.size() + " capacity:" + length);
    String declaringClass = this.elementType.getCanonicalName();
    String var = Variable.classToVariableName(outputType) + newVar.index;

    b.append(declaringClass + "[] "
             + var
             + " = new " + declaringClass + "[] { ");
    for (int i = 0; i < inputVars.size(); i++) {
      if (i > 0)
        b.append(", ");

      // In the short output format, statements like "int x = 3" are not added to a sequence; instead,
      // the value (e.g. "3") is inserted directly added as arguments to method calls.
      Operation statementCreatingVar = inputVars.get(i).getDeclaringStatement();
      if (!GenInputsAbstract.long_format
          && ExecutableSequence.canUseShortFormat(statementCreatingVar)) {
        b.append(PrimitiveTypes.toCodeString(((NonreceiverTerm) statementCreatingVar).getValue()));
      } else {
        b.append(inputVars.get(i).getName());
      }
    }
    b.append("};");
    b.append(Globals.lineSep);
  }

  @Override
  public int hashCode() {
    if (!hashCodeComputed) {
      hashCodeComputed = true;
      hashCodeCached = this.elementType.hashCode();
      hashCodeCached += this.length * 17;
    }
    return hashCodeCached;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ArrayCreation))
      return false;
    if (this == o)
      return true;
    ArrayCreation otherArrayDecl = (ArrayCreation) o;
    if (!this.elementType.equals(otherArrayDecl.elementType))
      return false;
    if (this.length != otherArrayDecl.length)
      return false;
    return true;
  }

  public String toParseableString() {
    return elementType.getName() + "[" + Integer.toString(length) + "]";
  }

  /**
   * A string representing this array declaration. The string is of the form:
   *
   * TYPE[NUMELEMS]
   *
   * Where TYPE is the type of the array, and NUMELEMS is the number of elements.
   *
   * Example:
   *
   * int[3]
   *
   */
  public static Operation parse(String str) {
    int openBr = str.indexOf('[');
    int closeBr = str.indexOf(']');
    String elementTypeStr = str.substring(0, openBr);
    String lengthStr = str.substring(openBr + 1, closeBr);
    Class<?> elementType = Reflection.classForName(elementTypeStr);
    int length = Integer.parseInt(lengthStr);
    return new ArrayCreation(elementType, length);
  }
}