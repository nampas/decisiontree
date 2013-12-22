
/**
 * Represents a datum for our decision tree- a member of the House of Representatives
 * @author Nathan P
 *
 */
class Representative {

	private String mIdentifier; // This politician's unique ID
	private char mParty; // Politiciain's party affiliation
	private String mVotes; // The voting record
	
	public Representative(String id, char party, String votes) {
		mParty = party;
		mIdentifier = id;
		mVotes = votes;
	}
	
	public void setVotes(String votes) {
		mVotes = votes;
	}
	
	public String getId() {
		return mIdentifier;
	}
	
	public String getVotes() {
		return mVotes;
	}
	
	public char getVote(int i) {
		return mVotes.charAt(i);
	}
	
	public char getParty() {
		return mParty;
	}
	
	public boolean identicalVoteRecords(Representative compare) {
		return mVotes.equals(compare.getVotes());
	}
}
