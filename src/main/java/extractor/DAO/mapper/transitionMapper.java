package extractor.DAO.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import extractor.model.transition;
@Mapper
@Component
public interface transitionMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table transition
     *
     * @mbg.generated Sat Apr 27 21:00:48 CST 2019
     */
    int deleteByPrimaryKey(Integer transitionid);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table transition
     *
     * @mbg.generated Sat Apr 27 21:00:48 CST 2019
     */
    int insert(transition record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table transition
     *
     * @mbg.generated Sat Apr 27 21:00:48 CST 2019
     */
    int insertSelective(transition record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table transition
     *
     * @mbg.generated Sat Apr 27 21:00:48 CST 2019
     */
    transition selectByPrimaryKey(Integer transitionid);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table transition
     *
     * @mbg.generated Sat Apr 27 21:00:48 CST 2019
     */
    int updateByPrimaryKeySelective(transition record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table transition
     *
     * @mbg.generated Sat Apr 27 21:00:48 CST 2019
     */
    int updateByPrimaryKey(transition record);
}