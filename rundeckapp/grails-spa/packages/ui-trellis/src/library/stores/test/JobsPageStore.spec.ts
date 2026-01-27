import { JobPageStore } from '../JobPageStore';
import { getRundeckContext } from '../../rundeckService';
import {
    bulkDeleteJobs,
    bulkExecutionEnableDisable,
    bulkScheduleEnableDisable,
    getProjectMeta,
} from '../../services/jobBrowse';
import {JobBrowseItem, JobBrowseMeta} from '../../types/jobs/JobBrowse';

// First, set up all mocks
jest.mock('../../rundeckService', () => ({
    getRundeckContext: jest.fn().mockImplementation(() => ({
        projectName: 'TestProject',
        rdBase: 'http://localhost:4440',
    })),
}));

jest.mock('../../services/jobBrowse', () => ({
    bulkDeleteJobs: jest.fn(),
    bulkExecutionEnableDisable: jest.fn(),
    bulkScheduleEnableDisable: jest.fn(),
    getProjectMeta: jest.fn(),
}));

describe('JobPageStore', () => {
    let store: JobPageStore;

    const mockJob: JobBrowseItem = {
        job: false,
        jobName: 'Test Job',
        groupPath: 'Test Group',
        id: 'job1',
        description: 'Test Description',
        meta: [
            {
                name: 'testMeta',
                data: { key: 'value' }
            }
        ]
    };

    const mockJob2: JobBrowseItem = {
        job: false,
        jobName: 'Test Job 2',
        groupPath: 'Test Group',
        id: 'job2',
        description: 'Test Description 2'
    };

    const mockMeta: JobBrowseMeta[] = [
        {
            name: 'authz',
            data: {
                types: {
                    job: {
                        delete: true,
                        edit: true,
                    },
                },
                project: {
                    admin: true,
                },
            },
        },
        {
            name: 'config',
            data: {
                executionsEnabled: true,
                scheduleEnabled: true,
                groupExpandLevel: 2,
            },
        },
        {
            name: 'sysMode',
            data: {
                active: true,
            },
        },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        store = new JobPageStore();
        (getProjectMeta as jest.Mock).mockResolvedValue(mockMeta);
    });

    describe('Bulk Job Operations', () => {
        describe('addBulkJob', () => {
            it('should add a job to selected jobs', () => {
                store.addBulkJob(mockJob);
                expect(store.selectedJobs).toHaveLength(1);
                expect(store.selectedJobs[0]).toBe(mockJob);
            });

            it('should not add duplicate jobs', () => {
                store.addBulkJob(mockJob);
                store.addBulkJob(mockJob);
                expect(store.selectedJobs).toHaveLength(1);
            });
        });

        describe('removeBulkJob', () => {
            beforeEach(() => {
                store.addBulkJob(mockJob);
            });

            it('should remove a job from selected jobs', () => {
                store.removeBulkJob(mockJob);
                expect(store.selectedJobs).toHaveLength(0);
            });

            it('should handle removing non-existent job', () => {
                const nonExistentJob: JobBrowseItem = {
                    job: false,
                    jobName: 'Non Existent',
                    groupPath: 'Test Group',
                    id: 'nonexistent'
                };
                store.removeBulkJob(nonExistentJob);
                expect(store.selectedJobs).toHaveLength(1);
            });
        });

        describe('addBulkJobs', () => {
            it('should add multiple jobs', () => {
                const jobs = [mockJob, mockJob2];
                store.addBulkJobs(jobs);
                expect(store.selectedJobs).toHaveLength(2);
            });
        });

        describe('removeBulkJobs', () => {
            it('should remove multiple jobs', () => {
                const jobs = [mockJob, mockJob2];
                store.addBulkJobs(jobs);
                store.removeBulkJobs(jobs);
                expect(store.selectedJobs).toHaveLength(0);
            });
        });
    });

    describe('performBulkAction', () => {
        beforeEach(() => {
            store.addBulkJob(mockJob);
        });

        it('should handle bulk delete successfully', async () => {
            (bulkDeleteJobs as jest.Mock).mockResolvedValue({ allsuccessful: true });
            await store.performBulkAction('delete');
            expect(bulkDeleteJobs).toHaveBeenCalledWith('TestProject', ['job1']);
        });

        it('should throw error on failed bulk delete', async () => {
            (bulkDeleteJobs as jest.Mock).mockResolvedValue({
                allsuccessful: false,
                failed: [{ message: 'Failed to delete' }],
            });

            await expect(store.performBulkAction('delete'))
                .rejects
                .toThrow('Some jobs could not be deleted');
        });

        it('should handle schedule enable/disable', async () => {
            (bulkScheduleEnableDisable as jest.Mock).mockResolvedValue({ allsuccessful: true });
            await store.performBulkAction('enable_schedule');
            expect(bulkScheduleEnableDisable).toHaveBeenCalledWith('TestProject', ['job1'], true);
        });

        it('should handle execution enable/disable', async () => {
            (bulkExecutionEnableDisable as jest.Mock).mockResolvedValue({ allsuccessful: true });
            await store.performBulkAction('enable_execution');
            expect(bulkExecutionEnableDisable).toHaveBeenCalledWith('TestProject', ['job1'], true);
        });

        it('should throw error on failed schedule enable/disable', async () => {
            (bulkScheduleEnableDisable as jest.Mock).mockResolvedValue({
                allsuccessful: false,
                failed: [{ id: 'job1', message: 'Failed to update schedule' }],
            });

            await expect(store.performBulkAction('enable_schedule'))
                .rejects
                .toThrow('Some jobs could not be updated');
        });

        it('should throw error on failed execution enable/disable', async () => {
            (bulkExecutionEnableDisable as jest.Mock).mockResolvedValue({
                allsuccessful: false,
                failed: [{ id: 'job1', message: 'Failed to update execution' }],
            });

            await expect(store.performBulkAction('enable_execution'))
                .rejects
                .toThrow('Some jobs could not be updated');
        });
    });

    describe('Project Loading and Meta', () => {
        it('should load project meta data', async () => {
            await store.load();
            expect(getProjectMeta).toHaveBeenCalledWith('TestProject');
            expect(store.loaded).toBe(true);
            expect(store.jobAuthz).toEqual({ delete: true, edit: true });
            expect(store.projAuthz).toEqual({ admin: true });
        });

        it('should not reload if already loaded for same project', async () => {
            await store.load();
            await store.load();
            expect(getProjectMeta).toHaveBeenCalledTimes(1);
        });

        it('should reload for different project', async () => {
            await store.load('Project1');
            await store.load('Project2');
            expect(getProjectMeta).toHaveBeenCalledTimes(2);
        });

        it('should set execution and schedule flags from config', async () => {
            await store.load();
            expect(store.projectExecutionsEnabled).toBe(true);
            expect(store.projectSchedulesEnabled).toBe(true);
            expect(store.groupExpandLevel).toBe(2);
        });

        it('should set execution mode from sysMode', async () => {
            await store.load();
            expect(store.executionMode).toBe(true);
        });

        it('should handle missing meta data gracefully', async () => {
            (getProjectMeta as jest.Mock).mockResolvedValue([]);
            await store.load();
            expect(store.jobAuthz).toEqual({});
            expect(store.projAuthz).toEqual({});
            expect(store.projectExecutionsEnabled).toBe(false);
            expect(store.projectSchedulesEnabled).toBe(false);
            expect(store.groupExpandLevel).toBe(0);
        });
    });

    describe('URL Generation', () => {
        beforeEach(() => {
            store.currentProject = 'TestProject';
        });

        it('should create correct project SCM action href', () => {
            const href = store.createProjectScmActionHref('commit', 'export');
            expect(href).toBe('http://localhost:4440/project/TestProject/scm/export/performAction?actionId=commit');
        });

        it('should create correct job creation href', () => {
            const href = store.createJobHref();
            expect(href).toBe('http://localhost:4440/project/TestProject/job/create');
        });

        it('should create correct job upload href', () => {
            const href = store.uploadJobHref();
            expect(href).toBe('http://localhost:4440/project/TestProject/job/upload');
        });

        it('should create correct job page path href', () => {
            const href = store.jobPagePathHref('test/path');
            expect(href).toBe('http://localhost:4440/project/TestProject/jobs/test/path');
        });
    });

    describe('Project and Browser Management', () => {
        it('should return current project', () => {
            store.currentProject = 'TestProject';
            expect(store.getProject()).toBe('TestProject');
        });

        it('should create and return job browser instance', () => {
            const browser = store.getJobBrowser();
            expect(browser).toBeDefined();
            expect(store.browser).toBe(browser);
        });

        it('should return same browser instance on subsequent calls', () => {
            const browser1 = store.getJobBrowser();
            const browser2 = store.getJobBrowser();
            expect(browser1).toBe(browser2);
        });
    });

    describe('loadProjAuthz', () => {
        it('should load and return project authorization', async () => {
            const projAuthz = await store.loadProjAuthz();
            expect(projAuthz).toEqual({ admin: true });
            expect(store.loaded).toBe(true);
        });

        it('should use cached data if already loaded', async () => {
            await store.load();
            const projAuthz = await store.loadProjAuthz();
            expect(getProjectMeta).toHaveBeenCalledTimes(1);
            expect(projAuthz).toEqual({ admin: true });
        });
    });
});