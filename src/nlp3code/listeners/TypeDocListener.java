package nlp3code.listeners;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

import nlp3code.DocHandler;
import nlp3code.InputHandler;
import nlp3code.recommenders.TypeRecommender;
import nlp3code.tester.TypeHandler;

/**
 * Listens for a type query during testing. Type queries end with $, 
 * this character is defined in TypeRecommender.testChar.
 */
public class TypeDocListener implements IDocumentListener {
	public static String previousQuery = null;
	
	@Override
	public void documentChanged(DocumentEvent event) {
//		QueryDocListener.currentDocument = DocHandler.getDocument();
//		DocHandler.documentChanged();
		
		//must be in testing state
		if(TypeRecommender.canTest == false) {
			//remove document listener if we got into a non testing state
			IDocument document = event.getDocument();
			document.removeDocumentListener(InputHandler.beginTestingListener);
		}
		
		String text = event.getText();
		if(text == "" || text.length() < 1) return;
		
		//get the line
		String line = DocHandler.getCurrentLine();
		if(line == null) return;
		
		String trimmed = line.trim();
		
		//check this isn't a previous query from an undo
		String checkUndo = trimmed;
		if (checkUndo.startsWith("?")) checkUndo = checkUndo.substring(1);
		if (checkUndo.endsWith("?")) checkUndo = checkUndo.substring(0, checkUndo.length()-1);
		if(checkUndo.equals(previousQuery)) {
			return;
		}
		
		//otherwise, lets check if we have a correctly formatted query
		if (!(trimmed.endsWith(TypeRecommender.testChar))) return;
		
		//add test case
		TypeHandler.constructTest(event.getOffset(), line);
		
		//remove document listener when done
		IDocument document = event.getDocument();
		document.removeDocumentListener(InputHandler.beginTestingListener);
		TypeRecommender.canRecommend = false;
		TypeRecommender.canTest = true;
		TypeRecommender.testing = true;
	}
	
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
	}
	
}