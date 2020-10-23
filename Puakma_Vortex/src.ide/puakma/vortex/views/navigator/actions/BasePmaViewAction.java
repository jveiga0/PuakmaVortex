/*
 * Author:  Martin Novak <mn@puakma.net>
 * Project: Puakma Vortex
 * Date:    Jan 5, 2005
 *
 * Copyright (c) 2004, 2005 webWise Network Consultants Pty Ltd, Australia,
 * http://www.wnc.net.au, all rights reserved
 *
 * Publishing, providing further or using of this program is prohibited
 * without previous written permission of author. Publishing or providing further
 * of the contents of this file is prohibited without prevous written permission
 * of the author.
 */
package puakma.vortex.views.navigator.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;

import puakma.vortex.views.navigator.PuakmaResourceView;


public abstract class BasePmaViewAction extends Action
{
  protected PuakmaResourceView view;
  
  BasePmaViewAction(PuakmaResourceView view, String title)
  {
    this.view = view;
    setText(title);
  }
  
  /**
   * Sets image descriptor from the shared images.
   *
   * @param imageName
   */
  protected void setSharedImage(String imageName)
  {
    ImageDescriptor imageDesc = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(imageName);
    setImageDescriptor(imageDesc);
  }
}
