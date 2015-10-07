package org.rundeck.plugin.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import org.junit.Test;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;

public class RankTieredOrchestatorTest {

    @Test
    public void testEmpty(){
        List<INodeEntry> nodes = new ArrayList<INodeEntry>();
        ExecutionContextImpl context = ExecutionContextImpl.builder()
                                                           .nodeRankAttribute("rank")
                                                           .nodeRankOrderAscending(true)
                                                           .build();
        RankTieredOrchestator plugin = new RankTieredOrchestator(context, nodes);

        //10 nodes 10% should equal one at a time
        assertEquals(null, plugin.nextNode());
    }

    @Test
    public void testRetrivalLogic(){
        INodeEntry node1 = create("host1", "rank", "1");
        INodeEntry node2 = create("host2", "rank", "1");

        INodeEntry node3 = create("host3", "rank", "2");

        INodeEntry node4 = create("host4", "rank", "3");

        INodeEntry node5 = create("host5", null, null);
        INodeEntry node6 = create("host6", null, null);
        
        List<INodeEntry> nodes = Arrays.asList(node1, node2, node3, node4, node5, node6);

        ExecutionContextImpl context = ExecutionContextImpl.builder()
                                                        .nodeRankAttribute("rank")
                                                        .nodeRankOrderAscending(true)
                                                        .build();

        RankTieredOrchestator plugin = new RankTieredOrchestator(context, nodes);
        
        //should return nodes in first rank
        assertEquals(node1, plugin.nextNode());
        assertEquals(node2, plugin.nextNode());

        //hit end of nodes with rank 1
        assertNull(plugin.nextNode());
        plugin.returnNode(node1, true, null);

        //still not complete with rank 1
        assertNull(plugin.nextNode());

        plugin.returnNode(node2, true, null);

        //can get the next node
        assertEquals(node3, plugin.nextNode());

        //no more at rank 2
        assertNull(plugin.nextNode());
        
        //return the rank 2 node
        plugin.returnNode(node3, true, null);
        
        //get the rank 3 node
        assertEquals(node4, plugin.nextNode());

        //no more nodes at rank 3
        assertEquals(null, plugin.nextNode());

        plugin.returnNode(node4, true, null);

        //now all unassigned ranks are available
        assertEquals(node5, plugin.nextNode());
        assertEquals(node6, plugin.nextNode());
        
        //check returns null since 2 currently being processed
        assertNull(plugin.nextNode());
        
        plugin.returnNode(node5, true, null);
        plugin.returnNode(node6, true, null);
        //check no more nodes
        assertNull(plugin.nextNode());
    }


    private INodeEntry create(String hostname, final String rankAttr, final String rankValue){
        INodeEntry iNodeEntry = NodeEntryImpl.create(hostname, hostname);

        if(null!=rankAttr) {
            iNodeEntry.getAttributes().put(rankAttr, rankValue);
        }
        return iNodeEntry;
    }
}
