package iotDemoCredentialSample;

import java.util.Map;

import org.apache.log4j.Logger;

import java.util.HashMap;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClient;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.model.GetIdRequest;
import com.amazonaws.services.cognitoidentity.model.GetIdResult;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityResult;
import com.amazonaws.services.cognitoidentity.model.Credentials;

public class IotDemoCredentials {

	static Logger logger = Logger.getLogger(IotDemoCredentials.class.getName());

	private String IDENTITY_POOL_ID;
	private Credentials credentials;

	public IotDemoCredentials(String identity_pool_id){

		this.IDENTITY_POOL_ID = identity_pool_id;
		AmazonCognitoIdentity cognito = new AmazonCognitoIdentityClient(new AnonymousAWSCredentials());
		
		/*
		 * Region‚ðŽw’è‚µ‚È‚¢‚ÆResource Not Found Exception‚ª‚Å‚é
		 */
		cognito.setRegion(Region.getRegion(Regions.AP_NORTHEAST_1));
		 
		GetIdResult getIdResult = cognito.getId(new GetIdRequest()
		  .withIdentityPoolId(IDENTITY_POOL_ID)
		  );
		 
		String identityId = getIdResult.getIdentityId();
		IotDemoCredentials.logger.info("identity ID = " + identityId);
		GetCredentialsForIdentityResult result = cognito.getCredentialsForIdentity(new GetCredentialsForIdentityRequest()
				.withIdentityId(identityId)
				);
		
		IotDemoCredentials.logger.info("AccessKetid = " + result.getCredentials().getAccessKeyId());
		IotDemoCredentials.logger.info("SecretKey = " + result.getCredentials().getSecretKey());
		IotDemoCredentials.logger.info("SessionToken = " + result.getCredentials().getSessionToken());
		
		this.credentials = result.getCredentials();

	}
	
	public Credentials getCredentials(){
		return this.credentials;
	}
}
