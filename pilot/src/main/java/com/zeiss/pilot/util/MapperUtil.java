package com.zeiss.pilot.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeanUtils;
import java.util.Arrays;

/**
 * Utilitário genérico para mapeamento entre objetos,
 * copiando apenas propriedades não-nulas da origem para o destino.
 */
public class MapperUtil {

	/**
	 * Copia propriedades não-nulas do objeto fonte para o objeto destino.
	 * Propriedades nulas na origem são ignoradas durante a cópia.
	 *
	 * @param source Objeto fonte
	 * @param target Objeto destino
	 * @param <S> Tipo do objeto fonte
	 * @param <T> Tipo do objeto destino
	 */
	public static <S, T> void copyNonNullProperties(S source, T target) {
		BeanUtils.copyProperties(source, target, getNullPropertyNames(source));
	}

	/**
	 * Retorna array com os nomes de propriedades nulas no objeto fornecido.
	 */
	private static String[] getNullPropertyNames(Object source) {
		BeanWrapper beanWrapper = new BeanWrapperImpl(source);
		return Arrays.stream(beanWrapper.getPropertyDescriptors())
				.map(pd -> pd.getName())
				.filter(name -> beanWrapper.getPropertyValue(name) == null)
				.toArray(String[]::new);
	}
}
