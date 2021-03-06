package com.sophos.demoserverless.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.sophos.demoserverless.model.Empleado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class EmpleadoRepository {

	public static final String HK_EMPLEADOS = "EMPLEADO";
	private static final String HK_LOG = "LOG";
	private static final String GSI_CEDULA = "cedula-index";
	private static final String KEY_CONDITION_EXPRESSION = "hk = :hk";

	private final DynamoDBMapper mapper;

	@Autowired
	public EmpleadoRepository(DynamoDBMapper mapper) {
		this.mapper = mapper;
	}

	public List<Empleado> findAll() {
		final Map<String, AttributeValue> eav = new HashMap<>();
		eav.put(":hk", new AttributeValue(HK_EMPLEADOS));

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<Empleado>()
			.withKeyConditionExpression(KEY_CONDITION_EXPRESSION)
			.withExpressionAttributeValues(eav)
		;

		return mapper.query(Empleado.class, queryExpression);
	}

	public Optional<Empleado> findById(UUID codigo) {
		return Optional.ofNullable(mapper.load(Empleado.class, HK_EMPLEADOS, codigo.toString()));
	}

	public Empleado save(Empleado empleado) {
		mapper.save(empleado);
		return empleado;
	}

	public void deleteById(UUID codigo) {
		final Empleado empleado = new Empleado();
		empleado.setHk(HK_EMPLEADOS);
		empleado.setSk(codigo.toString());
		mapper.delete(empleado);
	}

	public void deleteAllLogs(Collection<Empleado> logs) {
		mapper.batchDelete(logs);
	}

	public List<Empleado> findByCedula(String cedula) {
		final Empleado empleado = new Empleado();
		empleado.setCedula(cedula);

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<>();
		queryExpression.setHashKeyValues(empleado);
		queryExpression.setIndexName(GSI_CEDULA);
		queryExpression.setConsistentRead(false);

		return mapper.query(Empleado.class, queryExpression);
	}

	public List<Empleado> search(String query) {
		final AtomicInteger idx = new AtomicInteger();
		final Map<String, AttributeValue> eav = new HashMap<>();
		final String filterExpression = Stream.of(query.split("\\s*\\+\\s*"))
			.map(String::toLowerCase)
			.map(filtro -> {
				final String placeholder = ":var" + idx.incrementAndGet();
				eav.put(placeholder, new AttributeValue(filtro));
				return "contains(busqueda, " + placeholder + ")";
			})
			.collect(Collectors.joining(" and "))
		;
		eav.put(":hk", new AttributeValue(HK_EMPLEADOS));

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<Empleado>()
			.withKeyConditionExpression(KEY_CONDITION_EXPRESSION)
			.withFilterExpression(filterExpression)
			.withExpressionAttributeValues(eav)
		;

		return mapper.query(Empleado.class, queryExpression);
	}

	public List<Empleado> findLogsByCodigo(UUID codigo) {
		final Map<String, AttributeValue> eav = new HashMap<>();
		eav.put(":hk", new AttributeValue(HK_LOG));
		eav.put(":codigo", new AttributeValue(codigo.toString()));

		final DynamoDBQueryExpression<Empleado> queryExpression = new DynamoDBQueryExpression<Empleado>()
			.withKeyConditionExpression(KEY_CONDITION_EXPRESSION)
			.withFilterExpression("entidad.codigo = :codigo")
			.withExpressionAttributeValues(eav)
		;

		return mapper.query(Empleado.class, queryExpression);
	}

}
