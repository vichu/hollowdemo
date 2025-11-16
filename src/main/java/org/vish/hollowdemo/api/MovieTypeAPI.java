package org.vish.hollowdemo.api;

import com.netflix.hollow.api.custom.HollowObjectTypeAPI;
import com.netflix.hollow.core.read.dataaccess.HollowObjectTypeDataAccess;
import com.netflix.hollow.core.write.HollowObjectWriteRecord;

@SuppressWarnings("all")
public class MovieTypeAPI extends HollowObjectTypeAPI {

    private final MovieDelegateLookupImpl delegateLookupImpl;

    public MovieTypeAPI(MovieAPI api, HollowObjectTypeDataAccess typeDataAccess) {
        super(api, typeDataAccess, new String[] {
            "id",
            "title",
            "genre",
            "rating",
            "releaseYear",
            "duration",
            "description",
            "director",
            "cast",
            "writers",
            "productionCompany",
            "country",
            "language",
            "budget",
            "boxOffice",
            "tags",
            "ageRating",
            "awards"
        });
        this.delegateLookupImpl = new MovieDelegateLookupImpl(this);
    }

    public long getId(int ordinal) {
        if(fieldIndex[0] == -1)
            return missingDataHandler().handleLong("Movie", ordinal, "id");
        return getTypeDataAccess().readLong(ordinal, fieldIndex[0]);
    }

    public Long getIdBoxed(int ordinal) {
        long l;
        if(fieldIndex[0] == -1) {
            l = missingDataHandler().handleLong("Movie", ordinal, "id");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[0]);
            l = getTypeDataAccess().readLong(ordinal, fieldIndex[0]);
        }
        if(l == Long.MIN_VALUE)
            return null;
        return Long.valueOf(l);
    }



    public int getTitleOrdinal(int ordinal) {
        if(fieldIndex[1] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "title");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[1]);
    }

    public StringTypeAPI getTitleTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getGenreOrdinal(int ordinal) {
        if(fieldIndex[2] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "genre");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[2]);
    }

    public StringTypeAPI getGenreTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public double getRating(int ordinal) {
        if(fieldIndex[3] == -1)
            return missingDataHandler().handleDouble("Movie", ordinal, "rating");
        return getTypeDataAccess().readDouble(ordinal, fieldIndex[3]);
    }

    public Double getRatingBoxed(int ordinal) {
        double d;
        if(fieldIndex[3] == -1) {
            d = missingDataHandler().handleDouble("Movie", ordinal, "rating");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[3]);
            d = getTypeDataAccess().readDouble(ordinal, fieldIndex[3]);
        }
        return Double.isNaN(d) ? null : Double.valueOf(d);
    }



    public int getReleaseYear(int ordinal) {
        if(fieldIndex[4] == -1)
            return missingDataHandler().handleInt("Movie", ordinal, "releaseYear");
        return getTypeDataAccess().readInt(ordinal, fieldIndex[4]);
    }

    public Integer getReleaseYearBoxed(int ordinal) {
        int i;
        if(fieldIndex[4] == -1) {
            i = missingDataHandler().handleInt("Movie", ordinal, "releaseYear");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[4]);
            i = getTypeDataAccess().readInt(ordinal, fieldIndex[4]);
        }
        if(i == Integer.MIN_VALUE)
            return null;
        return Integer.valueOf(i);
    }



    public int getDuration(int ordinal) {
        if(fieldIndex[5] == -1)
            return missingDataHandler().handleInt("Movie", ordinal, "duration");
        return getTypeDataAccess().readInt(ordinal, fieldIndex[5]);
    }

    public Integer getDurationBoxed(int ordinal) {
        int i;
        if(fieldIndex[5] == -1) {
            i = missingDataHandler().handleInt("Movie", ordinal, "duration");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[5]);
            i = getTypeDataAccess().readInt(ordinal, fieldIndex[5]);
        }
        if(i == Integer.MIN_VALUE)
            return null;
        return Integer.valueOf(i);
    }



    public int getDescriptionOrdinal(int ordinal) {
        if(fieldIndex[6] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "description");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[6]);
    }

    public StringTypeAPI getDescriptionTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getDirectorOrdinal(int ordinal) {
        if(fieldIndex[7] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "director");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[7]);
    }

    public StringTypeAPI getDirectorTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getCastOrdinal(int ordinal) {
        if(fieldIndex[8] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "cast");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[8]);
    }

    public ListOfStringTypeAPI getCastTypeAPI() {
        return getAPI().getListOfStringTypeAPI();
    }

    public int getWritersOrdinal(int ordinal) {
        if(fieldIndex[9] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "writers");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[9]);
    }

    public ListOfStringTypeAPI getWritersTypeAPI() {
        return getAPI().getListOfStringTypeAPI();
    }

    public int getProductionCompanyOrdinal(int ordinal) {
        if(fieldIndex[10] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "productionCompany");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[10]);
    }

    public StringTypeAPI getProductionCompanyTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getCountryOrdinal(int ordinal) {
        if(fieldIndex[11] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "country");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[11]);
    }

    public StringTypeAPI getCountryTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getLanguageOrdinal(int ordinal) {
        if(fieldIndex[12] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "language");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[12]);
    }

    public StringTypeAPI getLanguageTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public long getBudget(int ordinal) {
        if(fieldIndex[13] == -1)
            return missingDataHandler().handleLong("Movie", ordinal, "budget");
        return getTypeDataAccess().readLong(ordinal, fieldIndex[13]);
    }

    public Long getBudgetBoxed(int ordinal) {
        long l;
        if(fieldIndex[13] == -1) {
            l = missingDataHandler().handleLong("Movie", ordinal, "budget");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[13]);
            l = getTypeDataAccess().readLong(ordinal, fieldIndex[13]);
        }
        if(l == Long.MIN_VALUE)
            return null;
        return Long.valueOf(l);
    }



    public long getBoxOffice(int ordinal) {
        if(fieldIndex[14] == -1)
            return missingDataHandler().handleLong("Movie", ordinal, "boxOffice");
        return getTypeDataAccess().readLong(ordinal, fieldIndex[14]);
    }

    public Long getBoxOfficeBoxed(int ordinal) {
        long l;
        if(fieldIndex[14] == -1) {
            l = missingDataHandler().handleLong("Movie", ordinal, "boxOffice");
        } else {
            boxedFieldAccessSampler.recordFieldAccess(fieldIndex[14]);
            l = getTypeDataAccess().readLong(ordinal, fieldIndex[14]);
        }
        if(l == Long.MIN_VALUE)
            return null;
        return Long.valueOf(l);
    }



    public int getTagsOrdinal(int ordinal) {
        if(fieldIndex[15] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "tags");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[15]);
    }

    public ListOfStringTypeAPI getTagsTypeAPI() {
        return getAPI().getListOfStringTypeAPI();
    }

    public int getAgeRatingOrdinal(int ordinal) {
        if(fieldIndex[16] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "ageRating");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[16]);
    }

    public StringTypeAPI getAgeRatingTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public int getAwardsOrdinal(int ordinal) {
        if(fieldIndex[17] == -1)
            return missingDataHandler().handleReferencedOrdinal("Movie", ordinal, "awards");
        return getTypeDataAccess().readOrdinal(ordinal, fieldIndex[17]);
    }

    public StringTypeAPI getAwardsTypeAPI() {
        return getAPI().getStringTypeAPI();
    }

    public MovieDelegateLookupImpl getDelegateLookupImpl() {
        return delegateLookupImpl;
    }

    @Override
    public MovieAPI getAPI() {
        return (MovieAPI) api;
    }

}