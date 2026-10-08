package com.taxi.easy.ua.utils.from_json_parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import retrofit2.Call;

@RunWith(MockitoJUnitRunner.class)
public class FromJSONParserRetrofitCallStateTest {

    @Test
    public void abandonedWhenCallMissing() {
        assertTrue(FromJSONParserRetrofit.isResponseAbandoned(
                FromJSONParserRetrofit.currentRequestGeneration(), null));
    }

    @Test
    public void abandonedWhenNewerRequestStarted() {
        Call<?> call = mock(Call.class);
        int newerGeneration = FromJSONParserRetrofit.currentRequestGeneration() + 1;
        assertTrue(FromJSONParserRetrofit.isResponseAbandoned(newerGeneration, call));
    }

    @Test
    public void abandonedWhenCallCanceled() {
        Call<?> call = mock(Call.class);
        when(call.isCanceled()).thenReturn(true);
        assertTrue(FromJSONParserRetrofit.isResponseAbandoned(
                FromJSONParserRetrofit.currentRequestGeneration(), call));
    }

    @Test
    public void keptWhenSameGenerationAndActive() {
        Call<?> call = mock(Call.class);
        when(call.isCanceled()).thenReturn(false);
        assertFalse(FromJSONParserRetrofit.isResponseAbandoned(
                FromJSONParserRetrofit.currentRequestGeneration(), call));
    }
}
