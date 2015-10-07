package org.rundeck.plugin.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.rundeck.plugin.example.MaxPercentageOrchestator;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;

public class MaxPercentageOrchestatorTest {

    @Test
    public void testMaxLogic(){
        List<INodeEntry> nodes = new ArrayList<INodeEntry>();
        MaxPercentageOrchestator plugin = new MaxPercentageOrchestator(null, nodes, 0);

        //10 nodes 10% should equal one at a time
        assertEquals(1, plugin.calculateMax(10, 10));

        //10 nodes 33% should equal 3
        assertEquals(3, plugin.calculateMax(10, 33));

        //10 nodes 1% should equal one at a time
        assertEquals(1, plugin.calculateMax(10, 1));

        //10 nodes 0% should default to 1 node
        assertEquals(1, plugin.calculateMax(10, 0));

        //7 nodes 50% should default to 3 node
        assertEquals(4, plugin.calculateMax(7, 50));
    }

    @Test
    public void testRetrivalLogic(){

        INodeEntry node1 = create("host1");
        INodeEntry node2 = create("host2");
        INodeEntry node3 = create("host3");
        INodeEntry node4 = create("host4");
        INodeEntry node5 = create("host5");
        INodeEntry node6 = create("host6");
        
        List<INodeEntry> nodes = Arrays.asList(node1, node2, node3, node4, node5, node6);
        
        MaxPercentageOrchestator plugin = new MaxPercentageOrchestator(null, nodes, 33);
        
        //should return nodes in order
        assertEquals(node1, plugin.nextNode());
        assertEquals(node2, plugin.nextNode());
        //hit our max will return node this thread would then stop
        assertNull(plugin.nextNode());
        plugin.returnNode(node1,true,null);
        //can get the next node
        assertEquals(node3, plugin.nextNode());
        assertNull(plugin.nextNode());
        
        //return the 2 nodes
        plugin.returnNode(node1,true,null);
        plugin.returnNode(node1,true,null);
        
        //get a single node and return it
        assertEquals(node4, plugin.nextNode());
        plugin.returnNode(node1,true,null);
        
        //get next 2 nodes
        assertEquals(node5, plugin.nextNode());
        assertEquals(node6, plugin.nextNode());
        
        //check returns null since 2 currently being processed
        assertNull(plugin.nextNode());
        
        plugin.returnNode(node1,true,null);
        plugin.returnNode(node1,true,null);
        //check no more nodes
        assertNull(plugin.nextNode());
    }


    private INodeEntry create(String hostname){
        return NodeEntryImpl.create(hostname, hostname);
    }
}
