package smartsuite.security.userdetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;

public class UserDetailsProxy extends User {
	
	static final long serialVersionUID = -3506314647963441971L;
	
	private User user;
	
	public UserDetailsProxy() {
		
	}
	
	public UserDetailsProxy(User user){
		this.user = user;
	}
	
	public String getUsername() {
		return user.getUsername();
	}

	public String getPassword() {
		return user.getPassword();
	}

	public boolean isAccountNonExpired() {
		return user.isAccountNonExpired();
	}

	public void setAccountNonExpired(boolean accountNonExpired) {
		user.setAccountNonExpired(accountNonExpired);
	}

	public boolean isAccountNonLocked() {
		return user.isAccountNonLocked();
	}

	public void setAccountNonLocked(boolean accountNonLocked) {
		user.setAccountNonLocked(accountNonLocked);
	}

	public boolean isCredentialsNonExpired() {
		return user.isCredentialsNonExpired();
	}

	public void setCredentialsNonExpired(boolean credentialsNonExpired) {
		user.setCredentialsNonExpired(credentialsNonExpired);
	}

	public boolean isCredentialsNonInitialized() {
		return user.isCredentialsNonInitialized();
	}

	public void setCredentialsNonInitialized(boolean credentialsNonInitialized) {
		user.setCredentialsNonInitialized(credentialsNonInitialized);
	}

	public boolean isEnabled() {
		return user.isEnabled();
	}

	public void setEnabled(boolean enabled) {
		user.setEnabled(enabled);
	}

	public Collection<GrantedAuthority> getAuthorities() {
		return user.getAuthorities();
	}

	public void setAuthorities(Collection<GrantedAuthority> authorities) {
		user.setAuthorities(authorities);
	}

	public Map<String, Object> getUserInfo() {
		return user.getUserInfo();
	}

	public void setUserInfo(Map<String, Object> userInfo) {
		user.setUserInfo(userInfo);
	}

	public List<Map<String, Object>> getUserRoles() {
		return user.getUserRoles();
	}

	public void setUserRoles(List<Map<String, Object>> userRoles) {
		user.setUserRoles(userRoles);
	}
	
	public List<Map<String, Object>> getUserOperationOrganizationCodes() {
		return user.getUserOperationOrganizationCodes();
	}
	
	public void setUserOperationOrganizationCodes(List<Map<String, Object>> userOperationOrganizationCodes) {
		user.setUserOperationOrganizationCodes(userOperationOrganizationCodes);
	}

	public boolean equals(Object o) {
		return user.equals(o);
	}

	public int hashCode() {
		return user.hashCode();
	}

	public String toString() {
		return user.toString();
	}

}
