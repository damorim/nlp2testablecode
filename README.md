# NLP2TestableCode
NLP2TestableCode is a plug-in for the Eclipse IDE that uses natural language tasks to search for relevent Java Stack Overflow snippets, corrects compiler errors, integrates code snippets by making changes based on existing source code and helps developers test code snippets.

## Plugin Installation Instructions:

To install the plugin for development:
 1. Download and install the Java Development Kit (JDK)
 2. Download and install Eclipse IDE for RCP and RAP Developers from the Eclipse Project website.
 3. File -> Import -> Git -> Projects from Git -> Clone URI.
 4. Copy and paste the .git URI from the NLP2TestableCode GitHub.
 5. Press Next until you get to the project import wizard. Choose "Import exisiting Eclipse projects" and press Next and Finish.
 6. Download CoreNLP (https://stanfordnlp.github.io/CoreNLP/) and extract into /lib
 7. Download the SO dataset (http://doi.org/10.5281/zenodo.3752789) and extract into /data
 8. You can now run the plugin by right-clicking launches/NLP2TestableCode.launch and selecting Run As... > NLP2TestableCode. If you recieve an error about the JRE, open 'Run Configurations...' in the Run menu and Eclipse should automatically update the JRE to your default.

To install the plugin on your regular Eclipse environment (e.g. for personal use), you will need to package the plugin so it can be installed via the Eclipse Install New Software tool. Since this repository is purely for the development of the tool, there is currently no support in this repository for packaging the plugin for installation.

## Important Plugin Configuration Settings:

### Content Assist:

To get the most out of the plugin, it is strongly recommended that you go to Window -> Preferences -> Java -> Editor -> Content Assist and:
- Turn off 'Insert single proposals automatically' so single type suggestions do not automatically trigger.
- Turn on 'Disable insertion triggers besides 'Enter''.
- Add a content assist binder to trigger the NLP2Code task content assist window by adding a '?' symbol to the set of symbols that trigger content assist.

### Required Libraries:

You will need to download Stanford CoreNLP (https://stanfordnlp.github.io/CoreNLP/) and extract the folder (stanford-corenlp-full-2018-10-05) into /lib.

### Stack Overflow Data:

NLP2TestableCode uses an offline database of SO posts, you will need to download the pre-filtered xml files from http://doi.org/10.5281/zenodo.3752789 and extract them into /data.

### Testing
To prevent the execution of arbirary code from within  the SO database, NLP2TestableCode has testing disabled by default. We do not recommend turning testing on without running the plug-in within a Virtual Machine. If you would like to turn testing on, the setting can be changed in Activator.java.

## How to use the plugin:

There are many ways to activate and use the plugin. 
Firstly, ensure that you are connected to the internet as this is needed to get Stack Overflow data.
To find a code snippet for a task you want to achieve, you can conduct natural language queries (note that the default language is Java for searching, however you can specifcy a different language by putting "in language" at the end of your query, e.g. "sort an array in python").

Queries can be invoked in three ways:
 1. Write your query on a line (e.g. sort an array) and press the Stack Overflow button on the toolbar (or pressing ctrl+6 as a hotkey).
 2. Construct a natural language query of characters and spaces with a question mark at the end (e.g. sort an array?).
 3. Pressing the keys you binded for the content assist during the setup to open the content assist and select one of the recommended tasks from the list. The results can be filtered by typing and similar tasks will be listed. If you chose not to set up a binding, you can cycle through content assist suppliers until you get to nlp2code.contentassist by pressing ctrl+space.

Once a query is invoked, the plugin will take a few seconds to collect and rank code snippets by relevance to your query. Once it is finished, the top result will be pasted into your document. If you find that the first result isn't helpful, you can cycle through all of the retrieved code snippets by pressing ctrl+` (ctrl + tilde/backtick key), or by pressing the stack overflow button with the blue arrow on the toolbar. Once you edit the document after a snippet has been pasted, you will be prompted for feedback on whether the query was successful or not.

tl;dr:
Conduct a query by:
 1. highlighting the text and pressing ctrl+6 (or pressing the stack overflow button).
 2. writing a query comprised of letters and spaces (no other characters accepted). e.g. sort an array?
 3. selecting a task in the content assist that suits what you are looking for.

After a query, cycle through possible solutions with ctrl + `.
After you select a snippet, you will be prompted for feedback if feedback has been enabled in the preferences.txt


## How to contribute:

Pull requests are most welcome!

## References:

For more information see:

NLP2TestableCode: Optimising the Fit of Stack Overflow Code Snippets intoExisting Code - https://arxiv.org/pdf/2004.07663.pdf

NLP2Code: Code Snippet Content Assistvia Natural Language Tasks - http://cs.adelaide.edu.au/~christoph/icsme17c.pdf
