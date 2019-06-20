package extractor.DAO.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import extractor.model.componenttransition;

@Mapper
@Component
public interface componenttransitionMapper {
	/**
	 * This method was generated by MyBatis Generator. This method corresponds to
	 * the database table componenttransition
	 *
	 * @mbg.generated Thu Jun 20 17:28:54 CST 2019
	 */
	int deleteByPrimaryKey(Integer idcomponenttransition);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to
	 * the database table componenttransition
	 *
	 * @mbg.generated Thu Jun 20 17:28:54 CST 2019
	 */
	int insert(componenttransition record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to
	 * the database table componenttransition
	 *
	 * @mbg.generated Thu Jun 20 17:28:54 CST 2019
	 */
	int insertSelective(componenttransition record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to
	 * the database table componenttransition
	 *
	 * @mbg.generated Thu Jun 20 17:28:54 CST 2019
	 */
	componenttransition selectByPrimaryKey(Integer idcomponenttransition);

	List<componenttransition> selectByComponent(Integer componentid);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to
	 * the database table componenttransition
	 *
	 * @mbg.generated Thu Jun 20 17:28:54 CST 2019
	 */
	int updateByPrimaryKeySelective(componenttransition record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to
	 * the database table componenttransition
	 *
	 * @mbg.generated Thu Jun 20 17:28:54 CST 2019
	 */
	int updateByPrimaryKey(componenttransition record);
}