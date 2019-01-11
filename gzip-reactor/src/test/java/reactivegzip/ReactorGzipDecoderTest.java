package reactivegzip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.async_.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactivegzip.domain.TestEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReactorGzipDecoderTest {

	private ObjectMapper objectMapper = new ObjectMapper();
	private ReactorGzipDecoder reader = new ReactorGzipDecoder(new JsonFactory());

	@Test
	public void shouldReadEntity() throws JsonProcessingException {
		TestEntity testEntity = new TestEntity(7, "testName");
		Publisher<ByteBuffer> byteBuffers = stringBuffer(objectMapper.writeValueAsString(testEntity));

		Mono<TestEntity> testEntityRed = reader.read(byteBuffers, objectMapper.readerFor(TestEntity.class));

		StepVerifier.create(testEntityRed)
				.expectNextMatches(testEntity_ -> testEntity_.equals(testEntity))
				.verifyComplete();
	}

	@Test
	public void shouldReadElements() throws JsonProcessingException {
		TestEntity[] testEntities = new TestEntity[]{
				new TestEntity(1, "testName1"),
				new TestEntity(3, "testName3"),
				new TestEntity(7, "testName7")};

		Publisher<ByteBuffer> byteBuffers = stringBuffer(objectMapper.writeValueAsString(testEntities));

		Flux<TestEntity> testEntityRed = reader.readElements(byteBuffers, objectMapper.readerFor(TestEntity.class));

		StepVerifier.create(testEntityRed)
				.expectNextMatches(testEntity -> testEntity.equals(testEntities[0]))
				.expectNextMatches(testEntity -> testEntity.equals(testEntities[1]))
				.expectNextMatches(testEntity -> testEntity.equals(testEntities[2]))
				.verifyComplete();
	}

	@Test
	public void shouldReadElementsAsArray() throws JsonProcessingException {
		TestEntity[] testEntities = new TestEntity[]{
				new TestEntity(1, "testName1"),
				new TestEntity(3, "testName3"),
				new TestEntity(7, "testName7")};
		Publisher<ByteBuffer> byteBuffers = stringBuffer(objectMapper.writeValueAsString(testEntities));

		Mono<TestEntity[]> testEntityRed = reader.read(byteBuffers, objectMapper.readerFor(TestEntity[].class));

		StepVerifier.create(testEntityRed)
				.expectNextMatches(testEntities_ -> Arrays.equals(testEntities_, testEntities))
				.verifyComplete();
	}

	private Publisher<ByteBuffer> stringBuffer(String value) {
		return Flux.fromIterable(divideArray(value.getBytes(StandardCharsets.UTF_8), 5))
				.map(ByteBuffer::wrap);
	}

	private static List<byte[]> divideArray(byte[] source, int chunksize) {

		List<byte[]> result = new ArrayList<>();
		int start = 0;
		while (start < source.length) {
			int end = Math.min(source.length, start + chunksize);
			result.add(Arrays.copyOfRange(source, start, end));
			start += chunksize;
		}

		return result;
	}

}
