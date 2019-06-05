package nlp2code;

import org.apache.logging.log4j.Logger;
import org.apache.commons.io.output.NullOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

/**
 * class IMCompiler
 * Handles in memory compilation of code snippets.
 */

class IMCompiler{
	static Logger logger;
	public Integer totalErrors;
	private JavaCompiler compiler;
	private List<String> options;
	private JavaFileManager fileManager;
	private String before;
	private String after;
	private String fullName = "Main";
	/*holds information about errors for this task*/
	public HashMap<String, Integer> errorKinds = new HashMap<String, Integer>();
	public HashMap<String, Integer> snippetsAffected = new HashMap<String, Integer>();
	public ArrayList<Integer> lineArray = new ArrayList<Integer>();
	private Boolean evaluating = false;
	public Integer errorFree = 0;
	public Integer finalLines;
	public Boolean o, n, l;
	
	/*Constructor, setup compiler*/
	public IMCompiler(String b, String a, Boolean order, Boolean neutrality, Boolean loop){
		logger = Activator.getLogger();
		compiler = new EclipseCompiler();
		options = Arrays.asList("-warn");
//		compiler = ToolProvider.getSystemJavaCompiler();
//		options = Arrays.asList("-Xlint");
		options = null;
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
		before = b;
		after = a;
		totalErrors = 0;
		o = order;
		n = neutrality;
		l = loop;
	}
	
	/*Returns snippet with least compiler errors*/
	public Vector<String> getLeastCompilerErrorSnippet(Vector<String> snippets) {
		Vector<String> snippet = new Vector<String>();
		totalErrors = 0;
		Integer errorCount;
		Integer storedCount = -1;
		Integer i = 0;
		finalLines = 0;
		Integer lines = 0;
		lineArray.clear();
		for(String s : snippets) {
			i++;
			logger.debug("Snippet " + i + ", ");
			//modify
			//s = modify(s);
			errorCount = compile(s);
			if(errorCount == 0) {
				errorFree++;
				lines = s.split("\n").length;
				finalLines += lines;
				lineArray.add(lines);
			}
			if(storedCount == -1) {
				snippet.add(s);
				storedCount = errorCount;
			}
			else {
				if(errorCount < storedCount) {
					snippet.clear();
					snippet.add(s);
					storedCount = errorCount;
				}
			}
		}
		
		logger.debug("total for task " + totalErrors);
		for(String key : errorKinds.keySet()) {
			logger.debug(", \"" + key + "\", " + errorKinds.get(key));
		}
		logger.debug("\n");
		
		return snippet;
	}
	
	/*Accepts a string of code, returns number of errors*/
	public int compile(String code) {
		HashMap<String, Integer> snippetErrorKinds = new HashMap<String, Integer>();
		//System.out.println("\n" + before+code+after + "\n");
		Integer errors;
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		
		//create file from string
		JavaFileObject file = new JavaSourceFromString("Main", before+code+after);
		//add to compilation units
		Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
		
		//set the writer to null here, eclipse compiler treats null as to system.err
		Writer out = new OutputStreamWriter(new NullOutputStream());
		
		//run compilation task
		CompilationTask task = compiler.getTask(out, fileManager, diagnostics, options, null, compilationUnits);

	    boolean success = task.call();
	    errors = 0;
	    //System.out.println("------------------");
	    for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
	    	//if error
	    	if(diagnostic.getKind() == Diagnostic.Kind.ERROR) {
		    	String eCode = diagnostic.getCode();
		        //get first line of message
		    	String message = diagnostic.getMessage(null);
		    	if(message.contains("\n")){
		    			message = message.substring(0, message.indexOf("\n"));
		    	}	
		    	
		    	message = message + " " + eCode;
		    	
		    	//if recording errors
		    	if(evaluating == false) {
		    		Integer errorCount = 1;
			    	//add to error kinds
			        if(errorKinds.containsKey(message)) {
			        	errorCount = errorCount + errorKinds.get(message);
			        	errorKinds.replace(message, errorCount);
			        	errorCount = snippetsAffected.get(message);
			        	snippetsAffected.replace(message, errorCount+1);
			        }
			        else {
			        	errorKinds.put(message, errorCount);
			        	snippetsAffected.put(message, 1);
			        }
			        
			        errorCount = 1;
			        //add to snippetErrorKinds
			        if(snippetErrorKinds.containsKey(message)) {
			        	errorCount = errorCount + snippetErrorKinds.get(message);
			        	snippetErrorKinds.replace(message, errorCount);
			        }
			        else {
			        	snippetErrorKinds.put(message, errorCount);
			        }
		    	}
		    	
		    	errors++;
	    	}
	    }
	    
	    //if recording errors
	    if(evaluating == false) {
		    totalErrors += errors;
		    logger.debug("total " + errors);
		    for(String key : snippetErrorKinds.keySet()) {
		    	logger.debug(", \"" + key + "\", " + snippetErrorKinds.get(key));
		    }
		    logger.debug("\n");
	    }
	    
	    if(success == true) {
//	    	try {
//	    		run();
//	    	} catch (Exception e) {
//	    		
//	    	}
	    }
	    //System.out.println("Success: " + success);
		
		return errors;
	}

	/*Accepts a string of code, returns modification with least errors*/
	public String modify(String code) {
		String finalSnippet, modified;
		Integer errors, compare;
		
		//get errors
		evaluating = true;
		errors = compile(code);
		
		//if snippet is error free, return original
		if(errors == 0) {
			evaluating = false;
			return code;
		}
		
		finalSnippet = deletion(code, errors, o, n, l);
		if(finalSnippet != code) {
			//logger.debug("MODIFYING SNIPPET:\n");
			//logger.debug("Original:\n---------------\n" + code + "\n--------------\n");
			//logger.debug("Modified:\n---------------\n" + finalSnippet + "\n--------------\n");
		}
		evaluating = false;
		return finalSnippet;
	}
	
	/*deletion algorithm*/
	public String deletion(String code, Integer errors, Boolean order, Boolean neutrality, Boolean passes) {
		String finalSnippet;
		String modified;
		Integer minErrors;
		Integer testErrors;
		Integer lines;
		Integer toDelete;
		Boolean endCondition;
		
		//get starting state
		finalSnippet = code;
		minErrors = errors;
		lines = code.split("\n").length;
		
		//passes loop, end when deletions no longer happen
		endCondition = false;
		while(endCondition == false) {
			toDelete = 1;
			if(order == true) toDelete = lines - 1;
			endCondition = true;
			
			//until we hit end of snippet
			modified = deleteLine(finalSnippet, toDelete);
			while(modified != null) {
				testErrors = compile(modified);
				//logger.debug("deleting line " + toDelete + "\n----------\n" + modified + "\n----------\n");
				//if deletion removed errors
				if(testErrors < errors && neutrality == false){
					//new best
					finalSnippet = modified;
					errors = testErrors;
					//logger.debug("Reduced errors to: " + errors + "\n----------\n");
					if(passes) endCondition = false;
				}
				else if(testErrors <= errors && neutrality == true){
					//new best
					finalSnippet = modified;
					errors = testErrors;
					//logger.debug("Reduced errors to: " + errors + "\n----------\n");
					if(passes) endCondition = false;
				}
				//if it didn't
				else {
					//skip line
					if(order == false) toDelete++;
					//logger.debug("Reverted, skipping line.\n----------\n");
				}
				if(order == true) toDelete--;
				//break loop if we've reduced errors to 0
				if(errors == 0) break;
				
				//next
				modified = deleteLine(finalSnippet, toDelete);
			}
			
			//end after 1 pass if passes not enabled
			if(!passes) endCondition = true;
			
		}
		
		return finalSnippet;
	}
	
	/*Modifiy by deleting lines*/
	public String deleteLine(String code, Integer line) {
		String modified;
		String[] lines;
		Integer length;
		
		//split the snippet by nl for lines
		lines = code.split("\n");
		length = lines.length;
		
		//if selected line longer, return null
		if(line >= length || line < 0) {
			return null;
		}
		
		//delete line
		modified = "";
		for(Integer i = 0; i<length; i++) {
			if(i != line) {
				modified += lines[i] + "\n";
			}
		}
		
		return modified;
	}
	
	/*Runs compiled code
	 * UNTESTED, DO NOT RUN OUTSIDE VM
	public void run() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            fileManager
                    .getClassLoader(null)
                    .loadClass(fullName)
                    .getDeclaredMethod("main", new Class[]{String[].class})
                    .invoke(null, new Object[]{null});
        } catch (InvocationTargetException e) {
            System.out.print("InvocationTargetException");
            //logger.error("InvocationTargetException:", e);
        } catch (NoSuchMethodException e) {
            System.out.print("NoSuchMethodException ");
            //logger.error("NoSuchMethodException:", e);
        }
    }
    */
}

	

class JavaClassObject extends SimpleJavaFileObject {
    protected final ByteArrayOutputStream bos =
            new ByteArrayOutputStream();

    public JavaClassObject(String name, Kind kind) {
        super(URI.create("string:///" + name.replace('.', '/')
                + kind.extension), kind);
    }

    public byte[] getBytes() {
        return bos.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return bos;
    }
}

class ClassFileManager extends ForwardingJavaFileManager {
    private JavaClassObject javaClassObject;

    public ClassFileManager(StandardJavaFileManager standardManager) {
        super(standardManager);
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return new SecureClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] b = javaClassObject.getBytes();
                return super.defineClass(name, javaClassObject.getBytes(), 0, b.length);
            }
        };
    }

    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        this.javaClassObject = new JavaClassObject(className, kind);
        return this.javaClassObject;
    }
}


class JavaSourceFromString extends SimpleJavaFileObject {
	  final String code;

	  JavaSourceFromString(String name, String code) {
	    super(URI.create("file:///" + name + Kind.SOURCE.extension),Kind.SOURCE);
	    this.code = code;
	  }

	  @Override
	  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
	    return code;
	  }
}