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

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
* Jenkins Plug-In for SASUnit, the unit testing framework for SAS.
* 
* This Plug-In runs SASUnit and executes SASUnit unit tests. If specified
* a doxygen documentation is created as well.
* 
* @author Bernhard Braun
* 
* @version 0.1.0.0
*/
public class SASUnitPlugInBuilder extends Builder{

   /**
   * SASUnit Version used to run the test 
   */
   private final String sasunitVersion;

   /**
   * Relative path to SASUnit batch file to be executed
   */
   private final String sasunitBatch;

   /**
   * Relative path to Doxygen batch file to be executed
   */
   private final String doxygenBatch;
   
   /**
   * Doxygen documentation is created if set to true
   */
   private final boolean createDoxygenDocu;
   
   /**
   * Constructor using fields
   *
   * @param sasunitBatch
   * Relative path to SASUnit batch file to be executed
   * @param doxygenBatch
   * Relative path to Doxygen batch file to be executed
   * @param sasunitVersion
   * SASUnit Version used to run the test
   * @param createDoxygenDocu
   * Doxygen documentation is created if set to true
   */
	@DataBoundConstructor
	public SASUnitPlugInBuilder(String sasunitBatch, String doxygenBatch, String sasunitVersion, boolean createDoxygenDocu) {
		this.sasunitBatch 		= sasunitBatch;
		this.doxygenBatch 		= doxygenBatch;
		this.sasunitVersion 	= sasunitVersion;
		this.createDoxygenDocu 	= createDoxygenDocu;
	}

	public String getSasunitBatch() {
		return sasunitBatch;
	}

	public String getDoxygenBatch() {
		return doxygenBatch;
	}

	public String getSasunitVersion() {
		return sasunitVersion;
	}

   public boolean isCreateDoxygenDocu() {
		return createDoxygenDocu;
	}
   
   /**
   * Method loops over all available SASUnit installations and returns the one specified in the project setup. 
   * If not found, the method returns null.
   * 
   * @return SASUnitInstallation
   */
	public SASUnitInstallation getInstallation() {
		if (sasunitVersion == null)
			return null;
		for (SASUnitInstallation i : DESCRIPTOR.getInstallations()) {
			if (sasunitVersion.equals(i.getName()))
				return i;
		}
		return null;
	}

   /*
   * (non-Javadoc)
   *
   * @see
   * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
   * , hudson.Launcher, hudson.model.BuildListener)
   */
   @Override
   public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
      
      List<Cause> buildStepCause = new ArrayList<Cause>();
      buildStepCause.add(new Cause() {
         public String getShortDescription() {
            return Messages.SASUnitPlugInBuilder_StartingSASUnitTestSuite();
         }
      });
      listener.started(buildStepCause);
      
      EnvVars env = build.getEnvironment(listener);
      
      SASUnitInstallation installation = getInstallation();
      if (installation == null) {
         listener.fatalError(Messages.SASUnitPlugInBuilder_SASUnitInstallationNotFound());
         return false;
      }
      installation = installation.forNode(Computer.currentComputer().getNode(), listener);
      installation = installation.forEnvironment(env);
      
      ArgumentListBuilder sasunitArgs = new ArgumentListBuilder();
      ArgumentListBuilder doxygenArgs = new ArgumentListBuilder();
      
      // Get relevant directories on node
      FilePath projectWorkspace 	  = build.getWorkspace();
      
      FilePath sasUnitBinFolder    = new FilePath(projectWorkspace, getSasunitBatch()).getParent();
      FilePath projectRunAll       = new FilePath(projectWorkspace, "run_all.log");
      FilePath doxygenBinFolder    = new FilePath(projectWorkspace, getDoxygenBatch()).getParent();
      FilePath sasUnitBin          = new FilePath(projectWorkspace, getSasunitBatch());
      // used Strings
      String sasUnitRoot 		   = installation.getHome();
      String sasUnitBatchFile      = new FilePath(projectWorkspace, getSasunitBatch()).getName();
      String doxygenBatchFile      = new FilePath(projectWorkspace, getDoxygenBatch()).getName();
      String [] cmd;
      // used booleans
      boolean useDoxygen			= createDoxygenDocu == true && doxygenBatch != null;
      boolean result;
      
      // Log to console
      PrintStream logger = listener.getLogger();
      logger.append(Messages.SASUnitPlugInBuilder_Folders())
	      .append("Project Workspace:  " + projectWorkspace.getRemote()  + "\n")
	      .append("sasUnitRoot:        " + sasUnitRoot                   + "\n")
	      .append("sasUnitBinFolder:   " + sasUnitBinFolder.getRemote()  + "\n")
	      .append("sasUnitBin:         " + sasUnitBin.getRemote()        + "\n")
	      .append("doxygenBinFolder:   " + doxygenBinFolder.getRemote()  + "\n")
	      
	      .append(Messages.SASUnitPlugInBuilder_Files())
	      .append("projectRunAll:      " + projectRunAll                 + "\n")
	      .append("sasUnitBatchFile:   " + sasUnitBatchFile              + "\n")
	      .append("doxygenBatchFile:   " + doxygenBatchFile              + "\n")
	      
	      .append(Messages.SASUnitPlugInBuilder_SASUnitVersion())
	      .append("SASUnit Version:    " + installation.getName()        + "\n")
	      .append("SASUnit Path:       " + installation.getHome()        + "\n")
      ;

      // Execute SASUnit batch file
      logger.append(Messages.SASUnitPlugInBuilder_startingTest());
      // Linux / Unix
      if (launcher.isUnix()) {
 		 sasunitArgs.add("./" + sasUnitBatchFile);
 		 sasunitArgs.add("\"" + sasUnitRoot + "\"");
		 
		 cmd = sasunitArgs.toCommandArray();
      }
      //Windows
      else{
    	 sasunitArgs.add("cmd.exe", "/C");
    	// add an extra set of quotes after cmd/c to handle paths with spaces in Windows
    	 sasunitArgs.add("\"");
		 sasunitArgs.add(sasUnitBatchFile);

		 sasunitArgs.add(sasUnitRoot);
		 
		 sasunitArgs.add("\"");
 	 
		 cmd = sasunitArgs.toCommandArray();
      }
      result = execCmdJob(cmd, launcher, listener, sasUnitBinFolder);
      if ( result == false){
         return false;
      }
      
      // Create Doxygen documentation if checked
      if(useDoxygen == true){
         logger.append(Messages.SASUnitPlugInBuilder_StartingDoxygen());
         
         // Linux / Unix
         if (launcher.isUnix()) {
        	 doxygenArgs.add("./" + doxygenBatchFile);
        	 
        	 cmd = doxygenArgs.toCommandArray();
         }
         //Windows
         else{
        	 doxygenArgs.add("cmd.exe", "/C");
        	 doxygenArgs.add(doxygenBatchFile);
        	 
        	 cmd = doxygenArgs.toCommandArray();
         }
         result = execCmdJob(cmd, launcher, listener, sasUnitBinFolder);
         if (result == false){
            return false;
         }
      }
      logger.append(Messages.SASUnitPlugInBuilder_EndSASUnitTestSuite());
      listener.finished(Result.SUCCESS);
      return true;
   }
   
   /**
   * Method to execute commands in a linux shell or windows cmd window. Used to start creation of
   * SASUnit and Doxygen reports in batch mode.
   * 
   * @param cmd 			The String to be executed in a linux shell or windows cmd window
   * @param launcher 	The Launcher of the Build
   * @param listener 	The BuildListender that receives events that happen during the build
   * @return boolean
   */
	public boolean execCmdJob(String[] cmd, Launcher launcher, BuildListener listener, FilePath rootFolder) {
		System.out.println();
		for (int i = 0; i < cmd.length; i++) {
			System.out.print(cmd[i]);
		}
		System.out.println();
		System.out.println();
		try {
			int r;
			r = launcher.launch().cmds(cmd).stdout(listener).pwd(rootFolder).join();
			if (r != 0) {
				listener.finished(Result.FAILURE);
				return false;
			}
		} catch (IOException ioe) {
			ioe.printStackTrace(listener.fatalError(Messages.SASUnitPlugInBuilder_Execution() + " " + cmd + " "	+ Messages.SASUnitPlugInBuilder_NotSuccessful()));
			listener.finished(Result.FAILURE);
			return false;
		} catch (InterruptedException ie) {
			ie.printStackTrace(listener.fatalError(Messages.SASUnitPlugInBuilder_Execution() + " " + cmd + " " + Messages.SASUnitPlugInBuilder_NotSuccessful()));
			listener.finished(Result.FAILURE);
			return false;
		}
		return true;
	}
   
   @Override
   public Descriptor<Builder> getDescriptor() {
      return DESCRIPTOR;
   }
   
   /**
   * Descriptor should be singleton.
   */
   @Extension
   public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

   /**
   * Descriptor for {@link SASUnitPlugInBuilder}. Used as a singleton.
   * This descriptor persists all SASUnit installations configured
   */
   public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
      
      @CopyOnWrite
      private volatile SASUnitInstallation[] installations = new SASUnitInstallation[0];

      /*
      * (non-Javadoc)
      *
      * @see hudson.model.Descriptor#getDisplayName()
      */
      DescriptorImpl() {
         super(SASUnitPlugInBuilder.class);
         load();
      }

      /**
      * Performs on-the-fly validation.
      * 
      * @param  value This parameter receives the value that the user has typed.
      * @return Indicates the outcome of the validation. This is sent to the browser.
      */
      public FormValidation validateForm(@QueryParameter String value) throws IOException, ServletException {
         if (value.length() == 0)
         return FormValidation.error(Messages.SASUnitPlugInBuilder_PleaseSetPath());
         if (value.length() < 4)
         return FormValidation.warning(Messages.SASUnitPlugInBuilder_IsntTheNameTooShort());
         return FormValidation.ok();
      }
      
      public FormValidation doCheckSasunitRoot(@QueryParameter String value) throws IOException, ServletException {
         return FormValidation.validateRequired(value);
      }
      public FormValidation doCheckSasunitBatch(@QueryParameter String value) throws IOException, ServletException {
         return validateForm(value);
      }
      public FormValidation doCheckDoxygenBatch(@QueryParameter String value) throws IOException, ServletException {
         return validateForm(value);
      }

      /**
      * This human readable name is used in the configuration screen.
      */
      @Override
      public String getDisplayName() {
         return Messages.SASUnitPlugInBuilder_ExecuteSASUnitTestSuite();
      }

      /**
      * Get the SASUnit installations.
      *
      * @return The list of installations
      */
      public SASUnitInstallation[] getInstallations() {
         return installations;
      }

      /**
      * Set the SASUnit installations.
      */
      public void setInstallations(SASUnitInstallation... installations) {
         this.installations = installations;
         save();
      }
      
      /**
      * Obtains the {@link SASUnitInstallation.DescriptorImpl} instance.
      */
      public SASUnitInstallation.DescriptorImpl getToolDescriptor() {
         return ToolInstallation.all().get(SASUnitInstallation.DescriptorImpl.class);
      }		

      /*
      * (non-Javadoc)
      *
      * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
      */
      @SuppressWarnings("rawtypes")
      @Override
      public boolean isApplicable(Class<? extends AbstractProject> jobType) {
         return true;
      }
   }
}