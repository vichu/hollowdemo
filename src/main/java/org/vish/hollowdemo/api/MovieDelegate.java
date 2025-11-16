package org.vish.hollowdemo.api;

import com.netflix.hollow.api.objects.delegate.HollowObjectDelegate;


@SuppressWarnings("all")
public interface MovieDelegate extends HollowObjectDelegate {

    public long getId(int ordinal);

    public Long getIdBoxed(int ordinal);

    public int getTitleOrdinal(int ordinal);

    public int getGenreOrdinal(int ordinal);

    public double getRating(int ordinal);

    public Double getRatingBoxed(int ordinal);

    public int getReleaseYear(int ordinal);

    public Integer getReleaseYearBoxed(int ordinal);

    public int getDuration(int ordinal);

    public Integer getDurationBoxed(int ordinal);

    public int getDescriptionOrdinal(int ordinal);

    public int getDirectorOrdinal(int ordinal);

    public int getCastOrdinal(int ordinal);

    public int getWritersOrdinal(int ordinal);

    public int getProductionCompanyOrdinal(int ordinal);

    public int getCountryOrdinal(int ordinal);

    public int getLanguageOrdinal(int ordinal);

    public long getBudget(int ordinal);

    public Long getBudgetBoxed(int ordinal);

    public long getBoxOffice(int ordinal);

    public Long getBoxOfficeBoxed(int ordinal);

    public int getTagsOrdinal(int ordinal);

    public int getAgeRatingOrdinal(int ordinal);

    public int getAwardsOrdinal(int ordinal);

    public MovieTypeAPI getTypeAPI();

}