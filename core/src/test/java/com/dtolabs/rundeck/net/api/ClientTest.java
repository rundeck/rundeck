package com.dtolabs.rundeck.net.api;

import com.dtolabs.rundeck.net.model.ProjectImportStatus;
import okhttp3.*;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.mock.BehaviorDelegate;
import retrofit2.mock.Calls;
import retrofit2.mock.MockRetrofit;
import retrofit2.mock.NetworkBehavior;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientTest {

    private Retrofit retrofit;
    private MockRetrofit mockRetrofit;

    @Before
    public void setup(){
        retrofit = new Retrofit.Builder()
                .baseUrl("http://mock.url")
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(JacksonConverterFactory.create()
                )
                .build();


        NetworkBehavior behavior =  NetworkBehavior.create();
        behavior.setFailurePercent(0);
        mockRetrofit = new MockRetrofit.Builder(retrofit)
                .networkBehavior(behavior)
                .build();
    }

    @Test
    public void okCall() throws IOException{
        BehaviorDelegate<RundeckApi> delegate = mockRetrofit.create(RundeckApi.class);
        MockRundeckApi mock = new MockRundeckApi(delegate,true);
        Client client = new Client("https://test.com", "admin");
        client.setApi(mock);
        File file = Mockito.mock(File.class);
        ProjectImportStatus resp = client.importProjectArchive("",file,true,true,false, false);
        assertNotNull(resp);
        assertEquals(true,resp.getResultSuccess());
    }
    @Test
    public void okCallWithUnsucess() throws IOException{
        BehaviorDelegate<RundeckApi> delegate = mockRetrofit.create(RundeckApi.class);
        MockRundeckApi mock = new MockRundeckApi(delegate,false);
        Client client = new Client("https://test.com", "admin");
        client.setApi(mock);
        File file = Mockito.mock(File.class);
        ProjectImportStatus resp = client.importProjectArchive("",file,true,true,false, false);
        assertNotNull(resp);
        assertEquals(false,resp.getResultSuccess());
    }

    @Test(expected = RuntimeException.class)
    public void notOkCall() throws IOException{
        BehaviorDelegate<RundeckApi> delegate = mockRetrofit.create(RundeckApi.class);
        MockRundeckApi mock = new MockRundeckApi(delegate,false, true);
        Client client = new Client("https://test.com", "admin");
        client.setApi(mock);
        File file = Mockito.mock(File.class);
        ProjectImportStatus resp = client.importProjectArchive("",file,true,true,false, false);
    }
}

class MockRundeckApi implements RundeckApi{
    private boolean success;
    private boolean fail;

    private final BehaviorDelegate<RundeckApi> delegate;

    public MockRundeckApi(BehaviorDelegate<RundeckApi> delegate, boolean success){
        this.success = success;
        this.delegate = delegate;
    }
    public MockRundeckApi(BehaviorDelegate<RundeckApi> delegate, boolean success, boolean fail){
        this.fail = fail;
        this.success = success;
        this.delegate = delegate;
    }

    public Call<ProjectImportStatus> importProjectArchive(String project,String jobUuidOption,
                                                          Boolean importExecutions,Boolean importConfig,
                                                          Boolean importACL,RequestBody body
    ){
        ProjectImportStatus resp = new ProjectImportStatus();
        resp.successful = this.success;
        if(!success){
            resp.aclErrors = new ArrayList<>();
            resp.aclErrors.add("Error");
        }
        if(fail){
            retrofit2.Response response = retrofit2.Response.error(401, ResponseBody.create(MediaType.parse("application/json") ,"{\"result\": {\"error\": {\"message\": \"error\"}}}"));
            return delegate.returning(Calls.response(response)).importProjectArchive(project,jobUuidOption,importExecutions,
                    importConfig,importACL,body);
        }
        return delegate.returningResponse(resp).importProjectArchive(project,jobUuidOption,importExecutions,
                importConfig,importACL,body);
    }
}