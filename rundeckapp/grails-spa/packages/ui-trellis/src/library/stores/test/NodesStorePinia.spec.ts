import { setActivePinia, createPinia } from 'pinia';
import { useNodesStore } from '../NodesStorePinia';
import { getAppLinks } from "../../rundeckService";
import { getNodes, getNodeTags } from "../../../app/components/job/resources/services/nodeServices";

// Mock external dependencies
jest.mock('@/library', () => ({
    getRundeckContext: jest.fn().mockImplementation(() => ({
        rdBase: 'http://localhost:4440/',
        apiVersion: '41',
        contextPath: '',
        projectName: 'TestProject',
    })),
}));
jest.mock('@/library/services/api', () => ({
    rundeckService: {
        get: jest.fn(),
        post: jest.fn(),
    },
}));
jest.mock('../../../app/components/job/resources/services/nodeServices');
jest.mock('../../rundeckService');
jest.mock('../NodeFilterLocalstore');

describe('NodesStore', () => {
    const mockNodes = [
        { nodename: 'node1', hostname: 'host1' },
        { nodename: 'node2', hostname: 'host2' },
    ];

    const mockTags = {
        tags: [
            { name: 'tag1', nodeCount: 1 },
            { name: 'tag2', nodeCount: 2 },
        ]
    };

    beforeEach(() => {
        setActivePinia(createPinia());
        // Reset all mocks
        jest.clearAllMocks();

        // Setup default mock implementations
        (getNodes as jest.Mock).mockResolvedValue({ allnodes: mockNodes });
        (getNodeTags as jest.Mock).mockResolvedValue(mockTags);
        (getAppLinks as jest.Mock).mockReturnValue({
            frameworkNodesQueryAjax: 'mock-url'
        });
    });

    describe('state', () => {
        it('should have initial state', () => {
            const store = useNodesStore();

            expect(store.entities).toEqual({});
            expect(store.nodenamesToDisplay).toEqual([]);
            expect(store.tags).toEqual([]);
            expect(store.maxSize).toBe(20);
            expect(store.nodeFilterStore).toBeDefined();
        });
    });

    describe('getters', () => {
        describe('currentNodes', () => {
            it('should return empty array when no nodes exist', () => {
                const store = useNodesStore();
                expect(store.currentNodes).toEqual([]);
            });

            it('should return only nodes that exist in entities', () => {
                const store = useNodesStore();
                store.entities = {
                    node1: mockNodes[0],
                    node2: mockNodes[1],
                };
                store.nodenamesToDisplay = ['node1', 'node2', 'node3'];

                expect(store.currentNodes).toEqual([mockNodes[0], mockNodes[1]]);
            });

            it('should maintain order based on nodenamesToDisplay', () => {
                const store = useNodesStore();
                store.entities = {
                    node1: mockNodes[0],
                    node2: mockNodes[1],
                };
                store.nodenamesToDisplay = ['node2', 'node1'];

                expect(store.currentNodes).toEqual([mockNodes[1], mockNodes[0]]);
            });
        });

        describe('total', () => {
            it('should return correct count of displayed nodes', () => {
                const store = useNodesStore();
                store.nodenamesToDisplay = ['node1', 'node2'];

                expect(store.total).toBe(2);
            });
        });
    });

    describe('actions', () => {
        describe('upsertNodes', () => {
            it('should add new nodes to entities', () => {
                const store = useNodesStore();
                store.upsertNodes(mockNodes);

                expect(store.entities).toEqual({
                    node1: mockNodes[0],
                    node2: mockNodes[1],
                });
            });

            it('should update existing nodes while preserving other properties', () => {
                const store = useNodesStore();
                const existingNode = {
                    nodename: 'node1',
                    hostname: 'old-host',
                    extraProp: 'value'
                };

                store.entities = { node1: existingNode };
                store.upsertNodes([{ nodename: 'node1', hostname: 'new-host' }]);

                expect(store.entities.node1).toEqual({
                    nodename: 'node1',
                    hostname: 'new-host',
                    extraProp: 'value',
                });
            });

            it('should update nodenamesToDisplay with new node list', () => {
                const store = useNodesStore();
                store.upsertNodes(mockNodes);

                expect(store.nodenamesToDisplay).toEqual(['node1', 'node2']);
            });
        });

        describe('fetchNodes', () => {
            it('should fetch and store nodes successfully', async () => {
                const store = useNodesStore();
                await store.fetchNodes();

                expect(getNodes).toHaveBeenCalledWith(
                    expect.objectContaining({
                        filter: store.nodeFilterStore.selectedFilter,
                        maxSize: store.maxSize,
                    }),
                    'mock-url'
                );
                expect(store.entities).toEqual({
                    node1: mockNodes[0],
                    node2: mockNodes[1],
                });
            });

            it('should throw error when fetch fails', async () => {
                const errorMessage = 'Network error';
                (getNodes as jest.Mock).mockRejectedValue(new Error(errorMessage));

                const store = useNodesStore();
                await expect(store.fetchNodes()).rejects.toThrow(
                    `Failed to fetch nodes: ${errorMessage}`
                );
            });

            it('should pass additional params to getNodes', async () => {
                const store = useNodesStore();
                const additionalParams = { project: 'test' };

                await store.fetchNodes(additionalParams);

                expect(getNodes).toHaveBeenCalledWith(
                    expect.objectContaining({
                        ...additionalParams,
                        filter: store.nodeFilterStore.selectedFilter,
                        maxSize: store.maxSize,
                    }),
                    'mock-url'
                );
            });
        });

        describe('fetchTags', () => {
            it('should fetch and store tags successfully', async () => {
                const store = useNodesStore();
                await store.fetchTags();

                expect(getNodeTags).toHaveBeenCalled();
                expect(store.tags).toEqual(mockTags.tags);
            });

            it('should throw error when fetch fails', async () => {
                const errorMessage = 'Network error';
                (getNodeTags as jest.Mock).mockRejectedValue(new Error(errorMessage));

                const store = useNodesStore();
                await expect(store.fetchTags()).rejects.toThrow(
                    `Failed to fetch tags: ${errorMessage}`
                );
            });
        });

        describe('clearResults', () => {
            it('should clear nodenamesToDisplay', () => {
                const store = useNodesStore();
                store.nodenamesToDisplay = ['node1', 'node2'];

                store.clearResults();

                expect(store.nodenamesToDisplay).toEqual([]);
            });

            it('should not affect entities', () => {
                const store = useNodesStore();
                store.entities = { node1: mockNodes[0] };
                store.nodenamesToDisplay = ['node1'];

                store.clearResults();

                expect(store.entities).toEqual({ node1: mockNodes[0] });
            });
        });
    });
});