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

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

@SuppressWarnings("serial")
public class SASUnitInstallation extends ToolInstallation implements NodeSpecific<SASUnitInstallation>, EnvironmentSpecific<SASUnitInstallation> {
   
   /**
   * Constructor using fields.
   *
   * @param name The name of the SASUnitInstallation
   * @param home The home folder for this SASUnitInstallation
   */
   @DataBoundConstructor
   public SASUnitInstallation(String name, String home) {
      super(name, home, null);
   }

   /**
   * Get the installation for the environment.
   *
   * @param environment The environment
   * @return The new installation
   */
   public SASUnitInstallation forEnvironment(EnvVars environment) {
      return new SASUnitInstallation(getName(), environment.expand(getHome()));
   }

   public SASUnitInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
      return new SASUnitInstallation(getName(), translateFor(node, log));
   }
   
   /**
   * Installation descriptor
   */
   @Extension
   public static final class DescriptorImpl extends ToolDescriptor<SASUnitInstallation> {

      /**
      * Check that the path to the SASUnit Installation is specified
      *
      * @param value The Folder to check
      */
      public FormValidation doCheckHome(@QueryParameter final File value) {
         // this can be used to check the existence of a file on the server, so needs to be protected
         if (Hudson.getInstance().hasPermission(Hudson.ADMINISTER) == false) {
            return FormValidation.ok();
         }
         if ("".equals(value.getPath())) {
            return FormValidation.ok();
         }
         //Warn if folder does not exist on Master
         if (value.exists() == false) {
            return FormValidation.warning(Messages.SASUnitPlugInBuilder_PathDoesNotExist());
         }
         if (value.isDirectory() == false) {
            return FormValidation.error(Messages.SASUnitPlugInBuilder_PathIsNoDirectory());
         }

         return FormValidation.ok();
      }

      /**
      * Check that the name to the SASUnit Installation is set
      *
      * @param value The Folder to check
      */
      public FormValidation doCheckName(@QueryParameter final String value) {
         return FormValidation.validateRequired(value);
      }
      
      @Override
      public String getDisplayName() {
         return Messages.SASUnitPlugInBuilder_DisplayName();
      }

      @Override
      public SASUnitInstallation[] getInstallations() {
         return Hudson.getInstance().getDescriptorByType(SASUnitPlugInBuilder.DescriptorImpl.class).getInstallations();
      }
      
      public DescriptorImpl getToolDescriptor() {
         return ToolInstallation.all().get(DescriptorImpl.class);
      }

      @Override
      public void setInstallations(SASUnitInstallation... installations) {
         Hudson.getInstance().getDescriptorByType(SASUnitPlugInBuilder.DescriptorImpl.class).setInstallations(installations);
      }
   }
}

