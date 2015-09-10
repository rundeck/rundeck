package com.dtolabs.rundeck.server.plugins.jobs

import com.dtolabs.rundeck.plugins.scm.JobExportReference
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobFileMapper

/**
 * Writes all changes to jobs to a file
 */
class TestJobChangeListener {
    String mappingPath
    JobFileMapper mapper
    void setup(){
        if(null==mapper){
            mapper=new PatternJobFileMapper(mappingPath: mappingPath)
        }
    }
    void jobChangeEvent(
            final JobReference original,
            final JobExportReference reference,
            final JobChangeEvent.JobChangeEventType event
    )
    {
        setup()
        println "TestJobListenerPlugin event ${event}: ${reference}"
        File origfile = mapper.fileForJob(original)
        File outfile = mapper.fileForJob(reference)
        System.err.println("Job event (${event}), writing to path: ${outfile}")
        switch (event){
            case JobChangeEvent.JobChangeEventType.DELETE:
                origfile.delete()
                break;

            case JobChangeEvent.JobChangeEventType.MODIFY_RENAME:
            case JobChangeEvent.JobChangeEventType.CREATE:
            case JobChangeEvent.JobChangeEventType.MODIFY:
                if(!origfile.getAbsolutePath().equals(outfile.getAbsolutePath())){
                    origfile.delete()
                }
                if(!outfile.getParentFile().exists()){
                    if(!outfile.getParentFile().mkdirs()){
                        System.err.println("Failed to create parent dirs for ${outfile}")
                    }
                }
                outfile.withOutputStream {out->
                    reference.jobSerializer.serialize('xml',out)
                }
        }
    }
}
