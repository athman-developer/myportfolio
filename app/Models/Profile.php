<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Profile extends Model
{
    /** @use HasFactory<\Database\Factories\ProfileFactory> */
    use HasFactory;

    /**
     * The attributes that are mass assignable.
     *
     * @var list<string>
     */
    protected $fillable = [
        'user_id',
        'avatar',
        'avatar_original_name',
        'avatar_mime_type',
        'avatar_size',
        'bio',
        'location',
        'website',
        'twitter',
        'github',
        'linkedin',
        'instagram',
        'facebook',
        'youtube',
        'discord',
        'phone',
        'birthday',
        'gender',
        'description',
        'profile_visibility',
        'show_email',
        'show_phone',
        'show_location',
        'show_social_links',
        'allow_messages',
    ];

    /**
     * Get the user that owns the profile.
     */
    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }
}