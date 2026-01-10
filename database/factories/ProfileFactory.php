<?php

namespace Database\Factories;

use App\Models\Profile;
use App\Models\User;
use Illuminate\Database\Eloquent\Factories\Factory;

class ProfileFactory extends Factory
{
    /**
     * The name of the factory's corresponding model.
     *
     * @var string
     */
    protected $model = Profile::class;

    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'user_id' => User::factory(),
            'avatar' => null,
            'avatar_original_name' => null,
            'avatar_mime_type' => null,
            'avatar_size' => null,
            'bio' => $this->faker->sentence(),
            'location' => $this->faker->city(),
            'website' => $this->faker->url(),
            'twitter' => $this->faker->userName(),
            'github' => $this->faker->userName(),
            'linkedin' => $this->faker->userName(),
            'instagram' => $this->faker->userName(),
            'facebook' => $this->faker->userName(),
            'youtube' => $this->faker->userName(),
            'discord' => $this->faker->userName(),
            'phone' => $this->faker->phoneNumber(),
            'birthday' => $this->faker->date(),
            'gender' => $this->faker->randomElement(['male', 'female', 'other', 'prefer not to say']),
            'description' => $this->faker->paragraph(),
            'profile_visibility' => $this->faker->randomElement(['public', 'private']),
            'show_email' => $this->faker->boolean(70),
            'show_phone' => $this->faker->boolean(30),
            'show_location' => $this->faker->boolean(80),
            'show_social_links' => $this->faker->boolean(90),
            'allow_messages' => $this->faker->boolean(80),
        ];
    }
}