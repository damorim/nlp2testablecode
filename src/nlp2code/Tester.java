package nlp2code;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.Type;

/* Class Tester
 * Handles Testing of code snippets through public function test
 */

class Tester{
	private static String before;
	private static String after;
	private static String snippet;
	private static String className;
	private static String functionName;
	private static IMCompiler compiler;
	private static JavaParser parser;
	private static ParseResult<BlockStmt> result;
	
	/*	Function to test a snippet
	 *  Returns the number of passed tests. */
	public static Integer test(String s, String b, String a, List<String> argumentTypes, String returnType) {
		
		//parse our snippet
		parser = new JavaParser();
		//s = "String s = \"1\";\n String b = \"2\";\n Integer i = 0;\n i = Integer.parseInt(s);\n System.out.print(i);\n";
		result = parser.parseBlock("{" + s + "}");
	
	
		//set code fragments
		snippet = s;
		before = b;
		after = a;
		
		//construct a test function
		String test = constructFunction(returnType, argumentTypes);
		//cannot construct function
		if(test == null) {
			System.out.println("Can't construct test function.");
			return 0;
		}
		
		System.out.println(test);
		
		
		//construct file
		String code = constructFile(test);
		//cannot construct file
		if(code == null) return 0;
		
		//try to compile our test
		compiler = new IMCompiler(false, false, false);
		compiler.evaluating = true;
		Integer errors = compiler.compile(code);
		compiler.evaluating = false;
		
		
//		System.out.println(errors);
//		for (Diagnostic diagnostic : compiler.diagnostics.getDiagnostics()) {
//			System.out.println(diagnostic.getMessage(null));
//		}
		
		//if compilation fails, we assume some type mismatch with test and say no passes
		if(errors != 0) {
			System.out.println("Compilation failed, errors: " + errors);
			return 0;
		}
		
		//attempt to run tests
		return run();
		
	}
	
	/* 
	 * Function run
	 *   Runs our test case.
	 */
	private static Integer run() {
		Integer passed = 0;
		Class[] cArgs = new Class[1];
		String[] args = Evaluator.testInput.toArray(new String[0]);
		cArgs[0] = String.class;
		
		//get method
		JavaFileManager fm = compiler.fileManager;
		try {
			Method method = fm.getClassLoader(null).loadClass(compiler.fullName).getDeclaredMethod(functionName, cArgs);
			method.setAccessible(true);
			Integer result = (Integer) method.invoke(null, Evaluator.testInput.get(0));
			//System.out.println(result);
			if(result == Integer.parseInt(Evaluator.testOutput)) {
				return 1;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return passed;
	}
	
	
	/* 
	 * Function constructFunction
	 *   Constructs a function for testing 
	 */
	private static String constructFunction(String returnType, List<String> argumentTypes) {
		List<String> arguments;
		
		JavaParser fileParser = new JavaParser();
		ParseResult<CompilationUnit> fileResult = fileParser.parse(before+after);
		CompilationUnit cu = fileResult.getResult().get();
		for (Node childNode : cu.getChildNodes()) {
			if(childNode instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) childNode;
				className = c.getNameAsString();
			}
		}
		
		String test = "";
		
		//get arguments
		arguments = getArguments(argumentTypes);
		//if we couldn't find all arguments, fail
		if(arguments == null) return null;
		
		//get return
		Integer e = addReturn(returnType);
		//couldn't find return, fail
		if(e != 0) return null;
		
		//construct signature
		//for now use test but to avoid conflicts check if free
		functionName = "test";
		test += "public static " + returnType + " " + functionName + "(";
		for(int i=0; i<arguments.size(); i++) {
			if(i != 0) test += ", ";
			test += argumentTypes.get(i);
			test += " ";
			test += arguments.get(i);
		}
		test += ")\n";
		
		
		//add contents from parser
		test += result.getResult().get().toString();
		
		return test;
	}

	
	/* Builds the class file from before, after and the test function */
	private static String constructFile(String test) {
		
		
		
		
		String code = "";
		
		//get the current file
		code = before + after;
		
		String[] lines = code.split("\n");
		Integer insertPos = findClass(lines);
		if(insertPos == -1) return null;
		
		//reconstruct
		code = "";
		for(int i=0; i<lines.length; i++) {
			if(i == insertPos) {
				code += test;
			}
			code += lines[i] + "\n";
		}
		
		return code;
	}
	
	/*Finds class in source code, returns number of line after*/
	private static Integer findClass(String[] lines) {
		Integer pos = -1;
		Boolean classStart = false;
		
		for(int i=0; i<lines.length; i++) {
			String line = lines[i].trim();
			if(line.startsWith("class ")) {
				classStart = true;
				className = line.split(" ")[1];
				if(line.endsWith("{")) {
					classStart = false;
					pos = i+1;
					return pos;
				}
			}
			else if(classStart = true) {
				if(line.endsWith("{")) {
					pos = i+1;
					return pos;
				}
			}
		}
		
		return pos;
	}
	
	/*
	 * Function getArguments
	 *   Parse snippet to find arguments. Return null if this fails.
	 *  
	 *   Currently, number of arguments and their types are supplied.
	 *   We work from the assumption that a snippet declares important variables first.
	 */
	private static List<String> getArguments(List<String> argumentTypes) {
		List<String> arguments = new ArrayList<String>();
		
		//for each argument
		for(int i = 0; i<argumentTypes.size(); i++) {
			VariableDeclarator toRemove = null;
			Statement toRemoveS = null;
			Boolean toBreak = false;
			
			//for each statement
			List<Statement> statements = result.getResult().get().getStatements();
			for(Statement statement : statements) {
				//is our statement an expression
				if(statement.isExpressionStmt()) {
					Expression expression = statement.asExpressionStmt().getExpression();
					//is our expression a variable declaration?
					if(expression.isVariableDeclarationExpr()) {
						//get variables from declaration
						List<VariableDeclarator> vars = ((VariableDeclarationExpr) expression).getVariables();
						//go through all variables
						for(VariableDeclarator v : vars) {
							if(v.getType().toString().equals(argumentTypes.get(i))) {
								//add variable to arguments list
								arguments.add(v.getNameAsString());
								
								//remove to avoid recounting
								if(vars.size() > 1) toRemove = v;
								else toRemoveS = statement;
								
								
								//done for this argument type
								toBreak = true;
								break;
							}
						}
						//break for this argument type
						if(toBreak == true) break;
					}
				}
			}
			
			if(toRemove != null) toRemove.remove();
			if(toRemoveS != null) toRemoveS.remove();
		}
		
		//each argument type must have a corresponding argument
		if(arguments.size() != argumentTypes.size()) {
			return null;
		}
		
		return arguments;
	}
	
	/* 
	 * Function addReturn
	 * 	 Searches for last statements to find a valid return.
	 * 	 Returns 0 on success.
	 */
	private static Integer addReturn(String type) {
		List<Statement> statements = result.getResult().get().getStatements();
		
		//travel up looking for a return statement
		for(int i = statements.size()-1; i>=0; i--) {
			Statement statement = statements.get(i);
			
			//is statement an expression?
			if(statement.isExpressionStmt()) {
				Expression expression = statement.asExpressionStmt().getExpression();
				
				//1: A variable declaration
				if(expression.isVariableDeclarationExpr()) {
					List<VariableDeclarator> vars = expression.asVariableDeclarationExpr().getVariables();
					//go through list 
					for(int j=vars.size()-1; j>=0; j--) {
						//if matches our type
						if(vars.get(j).getTypeAsString().equals(type)){
							//append a return statement
							ReturnStmt returnStmt = new ReturnStmt((Expression)new NameExpr(vars.get(j).getName()));
							result.getResult().get().addStatement(returnStmt);
							
							return 0;
						}
					}
				}
				
				//2: an assignment
				if(expression.isAssignExpr()) {
					AssignExpr assign = expression.asAssignExpr();
					
					//construct our return statement from target
					ReturnStmt returnStmt = new ReturnStmt(assign.getTarget());
					result.getResult().get().addStatement(returnStmt);
					
					return 0;
				}
				
				//3: a method
				if(expression.isMethodCallExpr()) {
					MethodCallExpr methodCall = expression.asMethodCallExpr();
					
					//3.1 print statement, get argument
					if(methodCall.getScope().isPresent()) {
						if(methodCall.getScope().get().toString().equals("System.out")) {
							if(methodCall.getNameAsString().equals("print") || methodCall.getNameAsString().equals("println")) {
								
								//check if an argument exists
								if(!methodCall.getArguments().isEmpty()) {
									//construct return statement using
									ReturnStmt returnStmt = new ReturnStmt(methodCall.getArgument(0));
									//append
									result.getResult().get().addStatement(returnStmt);
									return 0;
								}
							}
						}
					}
					
					//3.2 other methods, append return
					else {
						//construct return statement
						ReturnStmt returnStmt = new ReturnStmt(expression);
						
						//remove the call
						statement.remove();
						
						//append the return
						result.getResult().get().addStatement(returnStmt);
						return 0;
					}
				}
			}
		}
		
		//if we never found a valid return
		return -1;
	}
	
}