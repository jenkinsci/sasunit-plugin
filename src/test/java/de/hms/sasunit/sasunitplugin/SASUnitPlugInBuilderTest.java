 /**
 * 
 *  Copyright (c) 2014, HMS Analytical Software GmbH, Heidelberg
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.hms.sasunit.sasunitplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
* Test interaction of SASUnit plug-in with Jenkins core.
* @author Bernhard Braun
*/
public class SASUnitPlugInBuilderTest {
	
	@Rule 
	public JenkinsRule jenkinsRule = new JenkinsRule();
	
	FreeStyleProject		project;
	SASUnitPlugInBuilder	s;
	FreeStyleBuild			build;
	FilePath				projectWorkspace;
	FilePath				jenkinsWorkspace;
	
	String					sasunitRoot			= "sasunit";
	String					sasunitBatch		= "sasunit/example/bin/sasunit.9.3.linux.en.ci.sh";
	String					doxygenBatch		= "sasunit/example/bin/doxygen.sh";
	String					resource			= "SASUnit/resources";
	String					sasUnitVersion		= "SASUnit_1.3";
	
	String					style1              = "SASUnit/resources/style1.sas7bitm";
	String					style2              = "SASUnit/resources/style2.sas7bitm";
	String					textFile            = "SASUnit/resources/style.txt";

	boolean					createDoxygenDocu	= false;
	
   List<String> sasunitFolders		= Arrays.asList("SASUnit/examples/bin", resource);
   List<String> sasunitFiles		= Arrays.asList(style1, style2, textFile);
   List<String> workspaceFolders	= Arrays.asList("sasunitReport", "doxygenReport");
   
   @Before
   public void setUp() throws Exception {
   	project = jenkinsRule.createFreeStyleProject();
   	s = new SASUnitPlugInBuilder(sasunitBatch, doxygenBatch, sasUnitVersion, createDoxygenDocu);
   	project.getBuildersList().add(s);
   	build = project.scheduleBuild2(0).get();
   	
      projectWorkspace = build.getWorkspace();
      jenkinsWorkspace = projectWorkspace.getParent();
   }
   
   public void createFileAndFolders() throws Exception {
   	
      FilePath projectWorkspace = build.getWorkspace();
      FilePath jenkinsWorkspace = projectWorkspace.getParent();
      //Create file and folder structure
      for (String sasunitFolder : sasunitFolders){
      	jenkinsWorkspace.child(sasunitFolder).mkdirs();
      }
      
      for(String workspaceFolder : workspaceFolders){
      	projectWorkspace.child(workspaceFolder).mkdirs();
      }	 
   }

//	@Test
//	public void testExecCmdJob() throws Exception {
//		project.getBuildersList().add(new TestBuilder() {
//			@Override
//			public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException,IOException {
//				
//				String cmd;
//				String fileWin 		= "cmdFile.cmd";
//				String fileLinux 	= "cmdFile.sh";
//				String command 		= "echo This_is_a_system_command";
//
//				//create os command files
//				projectWorkspace.child(fileWin).write(command, "Cp850");
//				projectWorkspace.child(fileLinux).write(command, "UTF-8");
//				
//				//check files are created
//				assertWorkspaceFileExists(new FilePath(jenkinsWorkspace, fileWin).getRemote(), (Build<?, ?>)build);
//				assertWorkspaceFileExists(new FilePath(jenkinsWorkspace, fileLinux).getRemote(), (Build<?, ?>)build);
//				
//				//execute os command files
//				
//				// Linux / Unix
//				if (launcher.isUnix()) {
//					cmd = " ./" + command;
//				}
//				//Windows
//				else{
//					cmd = "cmd /c call " + command;
//				}
//				
//				assertEquals(true, s.execCmdJob(cmd, launcher, listener));
//				String logContent = FileUtils.readFileToString(build.getLogFile());
//				assertEquals(logContent,true, logContent.contains("This_is_a_system_command"));
//			
//				return true;
//			}
//		});
//		project.scheduleBuild2(0);
//	}
   
	@Test 
	public void testFileAndFolderSetup() throws Exception {

   	createFileAndFolders();

   	System.out.println(build.getDisplayName() + " Setup completed, beginning with asserts");

      //Testing setup of test files
      for (String sasunitFolder : sasunitFolders){
      	assertJenkinsWorkFileExists(sasunitFolder, build);
      }
      
      for(String workspaceFolder : workspaceFolders){
      	assertWorkspaceFileExists(workspaceFolder, build);
      }   	
	}
	
	@Test 
	public void testGetInstallation() throws Exception {
		project.getBuildersList().add(new TestBuilder() {
			@Override
			public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException,IOException {
		
				SASUnitInstallation installation = s.getInstallation();
				 if (installation == null) {
					 listener.fatalError(Messages.SASUnitPlugInBuilder_SASUnitInstallationNotFound());
					 return false;
				 }
				
				assertEquals(true, listener.getLogger().checkError());
			
				return true;
			}
		});
		project.scheduleBuild2(0);
	}
	
	public void assertJenkinsWorkFileExists(String path, Build<?,?> b) throws IOException, InterruptedException{
		assertTrue( b.getWorkspace().getParent().child(path).exists() );
	}  
	 
	private static void assertWorkspaceFileExists(String path, Build<?,?> b) throws IOException, InterruptedException {
		assertTrue( b.getWorkspace().child(path).exists() );
	}
	
	private static void assertJenkinsWorkFileExistsNot(String path, Build<?,?> b) throws IOException, InterruptedException {
		assertTrue( !b.getWorkspace().getParent().child(path).exists() );
	}
}