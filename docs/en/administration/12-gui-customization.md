% GUI Customization

You can modify some display features of the Rundeck GUI by setting
these properties in the [rundeck-config.properties](configuration.html#rundeck-config.properties) file:

-------------------------------------------------------------------------------
**Property**                                **Description**                      **Example**
----------------------                      ----------------------------------   ----------------
`rundeck.gui.title`                         Title shown in app header            Test App

`rundeck.gui.logo`                          Logo icon path relative to           test-logo.png
                                            webapps/rundeck/images dir           

`rundeck.gui.logo-width`                    Icon width for proper display (32px  32px
                                            is best)                             

`rundeck.gui.logo-height`                   Icon height for proper display (32px 32px
                                            is best)                             

`rundeck.gui.titleLink`                     URL for the link used by the app     http://rundeck.org
                                            header icon.                         

`rundeck.gui.helpLink`                      URL for the "help" link in the app   http://rundeck.org/
                                            header.                              docs

`rundeck.gui.realJobTree`                   Displaying a real tree in the Jobs   false
                                            overview instead of collapsing            
                                            empty groups. **Default: true**           

`rundeck.gui.startpage`                     Change the default page shown after  'jobs'
                                            login. values: 'run','jobs' or 
                                            'history'. Default: 'run'.

`rundeck.gui.execution.tail.lines.default`  Change the default number of lines   50
                                            shown in the execution page in tail 
                                            mode view. (Default: 20)

`rundeck.gui.execution.tail.lines.max`      Change the maximum number of lines   200
                                            shown in the execution page in tail 
                                            mode view. (Default: 100)

`rundeck.gui.enableJobHoverInfo`            Shows job information when the user  false
                                            hovers over a job name in various  
                                            pages. (Default: true)
-------------------------------------------------------------------------------
