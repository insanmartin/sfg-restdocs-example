package guru.springframework.sfgrestdocsexample.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import guru.springframework.sfgrestdocsexample.domain.Beer;
import guru.springframework.sfgrestdocsexample.repositories.BeerRepository;
import guru.springframework.sfgrestdocsexample.web.model.BeerDto;
import guru.springframework.sfgrestdocsexample.web.model.BeerStyleEnum;

//Needed to configure RestDocumentation
@ExtendWith( RestDocumentationExtension.class )
//Tells the framework to auto configure RestDocs
@AutoConfigureRestDocs( uriScheme="https", uriHost = "dev.springframework.in", uriPort= 80 )
//To bring up the Spring Runner context before running the tests
//we have to use the annotation @ExtendWith(SpringExtension.class)
//but @WebMvcTest has this annotation configured so there's no point to set it again
@WebMvcTest(BeerController.class)
@ComponentScan(basePackages = "guru.springframework.sfgrestdocsexample.web.mappers")
class BeerControllerTest {

	//Spring gets in an instance of mock mvc 
    @Autowired
    MockMvc mockMvc;

    //this object is used to get an object as a string with json format
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BeerRepository beerRepository;

    @Test
    void getBeerById() throws Exception {
        given(beerRepository.findById(any())).willReturn(Optional.of(Beer.builder().build()));

        //with the get method of restDocs we have to change the call
        //we are binding the parameter variable with it's value (like the controller method)
        //mockMvc.perform(get("/api/v1/beer/" + UUID.randomUUID().toString())
        mockMvc.perform(get("/api/v1/beer/{beerId}", UUID.randomUUID().toString())
        		//example of query parameter (the controller really doesn't expect parameters)
        		.param( "iscold", "yes" )
        		.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                //this code is to generate documentation
                .andDo( document( "v1/beer-get", 
                		//documentation related to parameters
                		pathParameters(
                				parameterWithName( "beerId" ).description( "UUID of desired beer to get." ) 
                		),
                		//documentation related to url query parameters
                		requestParameters(
                				parameterWithName( "iscold" ).description( "Is Beer Cold Query param." ) 
        				),
                		//documentation of the response (returned object)
                		//it's required to write all the fields  
                		responseFields(
                				fieldWithPath( "id" ).description( "Id of Beer" ),
                                fieldWithPath("version").description("Version number"),
                                fieldWithPath("createdDate").description("Date Created"),
                                fieldWithPath("lastModifiedDate").description("Date Updated"),
                                fieldWithPath("beerName").description("Beer Name"),
                                fieldWithPath("beerStyle").description("Beer Style"),
                                fieldWithPath("upc").description("UPC of Beer"),
                                fieldWithPath("price").description("Price"),
                                fieldWithPath("quantityOnHand").description("Quantity On hand")
                		)
                ) );
    }

    @Test
    void saveNewBeer() throws Exception {
        BeerDto beerDto =  getValidBeerDto();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        ConstrainedFields fields = new ConstrainedFields( BeerDto.class );
        
        mockMvc.perform(post("/api/v1/beer/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isCreated())
                .andDo( document( "v1/beer-new",
                		//documentation of the request object fields (new object sent)
                		//it's required to write all the fields, but they could be ignored
                		requestFields(
                				fields.withPath( "id" ).ignored(),
                				fields.withPath("version").ignored(),
                				fields.withPath("createdDate").ignored(),
                				fields.withPath("lastModifiedDate").ignored(),
                				fields.withPath("beerName").description("Name of the beer"),
                				fields.withPath("beerStyle").description("Style of Beer"),
                				fields.withPath("upc").description("Beer UPC").attributes(),
                				fields.withPath("price").description("Beer Price"),
                				fields.withPath("quantityOnHand").ignored()
                		    )
                ));
    }

    @Test
    void updateBeerById() throws Exception {
        BeerDto beerDto =  getValidBeerDto();
        String beerDtoJson = objectMapper.writeValueAsString(beerDto);

        mockMvc.perform(put("/api/v1/beer/" + UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(beerDtoJson))
                .andExpect(status().isNoContent());
    }

    BeerDto getValidBeerDto(){
        return BeerDto.builder()
                .beerName("Nice Ale")
                .beerStyle(BeerStyleEnum.ALE)
                .price(new BigDecimal("9.99"))
                .upc(123123123123L)
                .build();

    }

    
    private static class ConstrainedFields {

        private final ConstraintDescriptions constraintDescriptions;

        ConstrainedFields(Class<?> input) {
            this.constraintDescriptions = new ConstraintDescriptions(input);
        }

        private FieldDescriptor withPath(String path) {
        	//this is adding teh constraint information to the documentation
            return fieldWithPath(path).attributes(key("constraints").value(StringUtils
                    .collectionToDelimitedString(this.constraintDescriptions
                            .descriptionsForProperty(path), ". ")));
        }
    }
}